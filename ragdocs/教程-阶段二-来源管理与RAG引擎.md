# 阶段二教程：来源管理 + RAG 引擎

> 本教程将一步步教你实现 RAG 知识库的核心功能：用户上传文档 → 文档解析 → 文本分块 → 向量化 → 存入向量数据库。完成后，用户可以在笔记本中管理多个知识来源。

---

## 1. 这个功能完成了什么

完成后，系统具备以下能力：
- ✅ 用户可以**上传 PDF/TXT/MD/DOCX 文件**作为知识来源
- ✅ 用户可以**粘贴文本**作为知识来源
- ✅ 上传的文件会经过 **"解析 → 分块 → 向量化 → 存储"** 的完整 RAG 处理链路
- ✅ 每个笔记本有**独立的向量存储**，互不干扰
- ✅ 向量存储在应用关闭时**自动持久化**为 JSON 文件
- ✅ 前端三栏布局：左栏来源面板、中栏对话面板、右栏 Studio 面板

## 2. 为什么这么做

### 2.1 什么是 RAG？

**RAG（Retrieval-Augmented Generation，检索增强生成）** 是当前最主流的让 AI 基于私有数据回答问题的技术方案。

```
传统 LLM：用户提问 → LLM 直接回答（只能用训练数据，无法使用你的私有文档）
RAG：    用户提问 → 从你的文档中检索相关片段 → 把片段作为上下文发给 LLM → LLM 基于你的文档回答
```

### 2.2 RAG 的处理流程

```
阶段一：文档入库（本阶段实现）
  上传文件 → 解析文本 → 切分为小块 → 每块转为向量 → 存入向量数据库

阶段二：对话检索（下一阶段实现）
  用户提问 → 问题转为向量 → 从向量数据库中找最相似的块 → 作为上下文发给 LLM
```

### 2.3 为什么需要分块？

LLM 有 token 上下文限制（比如 4K/8K），不可能把整篇文档塞进去。所以我们把文档切成小块（比如 500 字一块），只检索最相关的几块放进 Prompt。

### 2.4 为什么用 InMemoryEmbeddingStore？

生产环境通常用 Milvus、Pinecone 等专业向量数据库。但本项目是毕业设计，用 LangChain4j 自带的内存向量存储 + JSON 持久化就足够了，**零依赖部署**。

---

## 3. 开始教程

### 3.1 创建实体类

> **目标**：创建数据库表对应的 Java 实体类

#### 3.1.1 Source.java — 来源实体

**文件位置**：`src/main/java/com/lyhm/airag/model/entity/Source.java`

**为什么需要这个类**：每个上传的文件或粘贴的文本都是一个"来源"，需要记录它的文件名、类型、大小、处理状态等信息。

```java
package com.lyhm.airag.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 知识来源实体类
 * 对应数据库表 source
 */
@Data                    // Lombok: 自动生成 getter/setter/toString
@Builder                 // Lombok: 支持 Builder 模式创建对象
@NoArgsConstructor       // Lombok: 无参构造
@AllArgsConstructor      // Lombok: 全参构造
@Table("source")         // MyBatis-Flex: 映射到 source 表
public class Source implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID，自增
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 所属笔记本 ID
     * 一个笔记本下可以有多个来源
     */
    private Long notebookId;

    /**
     * 所属用户 ID（冗余字段，方便权限校验）
     */
    private Long userId;

    /**
     * 文件名（上传的文件名或文本来源的标题）
     */
    private String fileName;

    /**
     * 文件类型：pdf/txt/md/docx/text
     */
    private String fileType;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件存储路径（仅文件上传有值）
     */
    private String filePath;

    /**
     * 文本内容（仅文本来源有值，文件来源由 Tika 解析）
     */
    @Column("content")
    private String content;

    /**
     * 向量分块数量
     * 记录文档被分成了多少个向量块
     */
    private Integer segmentCount;

    /**
     * 处理状态：processing(处理中) / completed(完成) / failed(失败)
     * 上传后先标记 processing，处理完成后改为 completed
     */
    private String status;

    /**
     * 错误信息（处理失败时记录原因）
     */
    private String errorMessage;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记 0-正常 1-删除
     */
    @Column(isLogicDelete = true)
    private Integer isDelete;
}
```

**关键注解解释**：
- `@Table("source")` — 告诉 MyBatis-Flex 这个类对应数据库中的 `source` 表
- `@Id(keyType = KeyType.Auto)` — 主键自增
- `@Column(isLogicDelete = true)` — 逻辑删除，删除时不会真正删除记录，只把 `isDelete` 设为 1
- `@Builder` — 让我们可以用 `Source.builder().fileName("xxx").build()` 的方式创建对象

---

#### 3.1.2 Report.java — 报告实体

**文件位置**：`src/main/java/com/lyhm/airag/model/entity/Report.java`

```java
package com.lyhm.airag.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 报告实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("report")
public class Report implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    /** 所属笔记本 ID */
    private Long notebookId;

    /** 所属用户 ID */
    private Long userId;

    /** 报告标题 */
    private String title;

    /** 报告类型：briefing/study_guide/faq/timeline/custom */
    private String reportType;

    /** 自定义 Prompt（仅 custom 类型有值） */
    private String customPrompt;

    /** 报告内容（Markdown 格式） */
    @Column("content")
    private String content;

    /** 使用的来源 ID 列表（逗号分隔） */
    private String sourceIds;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @Column(isLogicDelete = true)
    private Integer isDelete;
}
```

---

#### 3.1.3 Quiz.java — 测验实体

**文件位置**：`src/main/java/com/lyhm/airag/model/entity/Quiz.java`

```java
package com.lyhm.airag.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 测验实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("quiz")
public class Quiz implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    private Long notebookId;
    private Long userId;

    /** 测验标题 */
    private String title;

    /** 题目数量 */
    private Integer questionCount;

    /** 难度：easy/medium/hard */
    private String difficulty;

    /**
     * 题目列表（JSON 数组格式）
     * 
     * 格式示例：
     * [
     *   {
     *     "questionId": 1,
     *     "question": "题目文本",
     *     "options": [
     *       {"label": "A", "text": "选项内容"},
     *       {"label": "B", "text": "选项内容"},
     *       {"label": "C", "text": "选项内容"},
     *       {"label": "D", "text": "选项内容"}
     *     ],
     *     "correctAnswer": "A",
     *     "explanation": "解析说明"
     *   }
     * ]
     */
    @Column("questions")
    private String questions;

    /** 使用的来源 ID 列表（逗号分隔） */
    private String sourceIds;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @Column(isLogicDelete = true)
    private Integer isDelete;
}
```

**关键设计**：`questions` 字段以 JSON 字符串形式存储在 MySQL 的 TEXT 列中。这样做的好处是灵活——不需要额外建表来存储每道题。

---

#### 3.1.4 QuizRecord.java — 答题记录实体

**文件位置**：`src/main/java/com/lyhm/airag/model/entity/QuizRecord.java`

```java
package com.lyhm.airag.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 答题记录实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("quiz_record")
public class QuizRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id(keyType = KeyType.Auto)
    private Long id;

    /** 对应的测验 ID */
    private Long quizId;

    /** 答题用户 ID */
    private Long userId;

    /** 得分（百分制） */
    private Integer score;

    /** 答对题数 */
    private Integer correctCount;

    /** 总题数 */
    private Integer totalCount;

    /** 答题花费时间（秒） */
    private Integer timeCost;

    /** 答题详情（JSON 格式） */
    @Column("answers")
    private String answers;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @Column(isLogicDelete = true)
    private Integer isDelete;
}
```

---

### 3.2 创建枚举类

> **目标**：定义系统中用到的类型常量

#### 3.2.1 SourceTypeEnum.java

**文件位置**：`src/main/java/com/lyhm/airag/model/enums/SourceTypeEnum.java`

**为什么需要枚举**：避免在代码中到处写 "pdf"、"txt" 这样的魔法字符串，统一管理类型。

```java
package com.lyhm.airag.model.enums;

import lombok.Getter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 来源文件类型枚举
 */
@Getter
public enum SourceTypeEnum {

    PDF("PDF 文件", "pdf"),
    TXT("文本文件", "txt"),
    MD("Markdown", "md"),
    DOCX("Word 文档", "docx"),
    TEXT("粘贴文本", "text");

    private final String text;   // 中文描述
    private final String value;  // 存数据库的值

    SourceTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据value获取枚举
     * 用法：SourceTypeEnum.getEnumByValue("pdf") → SourceTypeEnum.PDF
     */
    public static SourceTypeEnum getEnumByValue(String value) {
        if (value == null) return null;
        for (SourceTypeEnum e : values()) {
            if (e.value.equals(value)) return e;
        }
        return null;
    }
}
```

#### 3.2.2 ReportTypeEnum.java 和 QuizDifficultyEnum.java

结构与 SourceTypeEnum 完全一致，只是枚举值不同：

```java
// ReportTypeEnum 的枚举值：
BRIEFING("简报文档", "briefing"),
STUDY_GUIDE("学习指南", "study_guide"),
FAQ("常见问题", "faq"),
TIMELINE("时间线", "timeline"),
CUSTOM("自定义报告", "custom");

// QuizDifficultyEnum 的枚举值：
EASY("简单", "easy"),
MEDIUM("中等", "medium"),
HARD("困难", "hard");
```

---

### 3.3 创建 Mapper 接口

> **目标**：创建数据库访问层

**为什么这么简单**：MyBatis-Flex 继承 `BaseMapper<T>` 后自动具备所有基础 CRUD 方法（增删改查），不需要写任何 SQL。

```java
// 文件: src/main/java/com/lyhm/airag/mapper/SourceMapper.java
package com.lyhm.airag.mapper;

import com.lyhm.airag.model.entity.Source;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SourceMapper extends BaseMapper<Source> {
    // BaseMapper 自动提供：
    // insert(entity)     — 插入
    // selectOneById(id)  — 按ID查询
    // selectListByQuery(queryWrapper)  — 条件查询
    // update(entity)     — 更新
    // deleteById(id)     — 删除
}
```

同理创建 `ReportMapper`、`QuizMapper`、`QuizRecordMapper`。

---

### 3.4 创建 DTO 和 VO

> **目标**：定义前后端数据传输格式

#### 3.4.1 什么是 DTO 和 VO？

```
DTO (Data Transfer Object)：前端 → 后端 的请求参数
  例：SourceTextAddRequest — 前端添加文本来源时传的参数

VO (View Object)：后端 → 前端 的响应数据
  例：SourceVO — 返回给前端展示的来源信息（排除了敏感字段如 content、filePath）
```

#### 3.4.2 SourceTextAddRequest.java

```java
package com.lyhm.airag.model.dto.source;

import lombok.Data;
import java.io.Serializable;

/**
 * 添加文本来源的请求参数
 */
@Data
public class SourceTextAddRequest implements Serializable {

    /** 所属笔记本 ID */
    private Long notebookId;

    /** 来源标题 */
    private String title;

    /** 文本内容 */
    private String content;
}
```

#### 3.4.3 SourceVO.java

```java
package com.lyhm.airag.model.vo;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 来源视图对象（返回给前端的数据）
 *
 * 注意：不包含 content（文本内容太大）和 filePath（安全考虑）
 */
@Data
public class SourceVO implements Serializable {

    private Long id;
    private Long notebookId;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private Integer segmentCount;
    private String status;
    private String errorMessage;
    private LocalDateTime createTime;
}
```

---

### 3.5 配置 Embedding 模型

> **目标**：配置 DashScope 的文本嵌入模型，用于将文本转为向量

**文件位置**：`src/main/java/com/lyhm/airag/config/EmbeddingConfig.java`

**文本嵌入是什么**：把一段文字变成一个"数字数组"（向量），使得语义相近的文字，对应的向量也相近。这是向量检索的基础。

```java
package com.lyhm.airag.config;

import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 文本嵌入模型配置
 * 
 * 使用阿里云 DashScope 的 text-embedding-v3 模型
 * 该模型可以将任意文本转换为 1024 维的向量表示
 */
@Configuration
public class EmbeddingConfig {

    // 从 application-local.yml 读取 API 密钥
    @Value("${dashscope.api-key}")
    private String apiKey;

    // 从 application-local.yml 读取模型名称
    @Value("${dashscope.embedding.model-name}")
    private String modelName;

    /**
     * 创建 EmbeddingModel Bean
     * 
     * 这个 Bean 会被 Spring 容器管理，
     * 在需要的地方通过 @Resource 注入使用
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        return QwenEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)  // 例如 "text-embedding-v3"
                .build();
    }
}
```

**对应的配置文件** `application-local.yml`：
```yaml
dashscope:
  api-key: sk-xxxxxxxxxxxxxxxx
  embedding:
    model-name: text-embedding-v3
```

---

### 3.6 配置向量存储管理器

> **目标**：管理每个笔记本独立的向量存储实例

**文件位置**：`src/main/java/com/lyhm/airag/config/VectorStoreConfig.java`

**为什么每个笔记本需要独立的向量存储**：如果所有笔记本共用一个向量存储，搜索"量子物理"时可能会返回其他笔记本中"历史"相关的结果。独立存储确保了数据隔离。

```java
package com.lyhm.airag.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 向量存储管理器
 *
 * 核心设计思路：
 * 1. 每个笔记本拥有独立的 InMemoryEmbeddingStore 实例
 * 2. 使用 ConcurrentHashMap 管理所有实例（线程安全）
 * 3. 支持从 JSON 文件加载/保存（持久化）
 * 4. 应用关闭时自动保存所有存储到磁盘
 */
@Slf4j
@Component
public class VectorStoreConfig {

    // 向量存储文件保存目录
    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    // 线程安全的 Map：key=笔记本ID，value=该笔记本的向量存储
    private final ConcurrentHashMap<Long, InMemoryEmbeddingStore<TextSegment>> stores
            = new ConcurrentHashMap<>();

    /**
     * 获取或创建笔记本的向量存储
     *
     * 查找逻辑：
     * 1. 先从内存 Map 中查找
     * 2. 内存没有，尝试从 JSON 文件加载
     * 3. JSON 文件也没有，创建新实例
     */
    public InMemoryEmbeddingStore<TextSegment> getOrCreateStore(Long notebookId) {
        return stores.computeIfAbsent(notebookId, id -> {
            // 尝试从文件加载
            Path storePath = getStorePath(id);
            if (Files.exists(storePath)) {
                try {
                    String json = Files.readString(storePath);
                    log.info("从文件加载向量存储: notebook_{}", id);
                    return InMemoryEmbeddingStore.fromJson(json);
                } catch (IOException e) {
                    log.error("加载向量存储失败: notebook_{}", id, e);
                }
            }
            // 创建新实例
            log.info("创建新的向量存储: notebook_{}", id);
            return new InMemoryEmbeddingStore<>();
        });
    }

    /**
     * 持久化指定笔记本的向量存储到 JSON 文件
     */
    public void persistStore(Long notebookId) {
        InMemoryEmbeddingStore<TextSegment> store = stores.get(notebookId);
        if (store == null) return;

        try {
            Path storePath = getStorePath(notebookId);
            Files.createDirectories(storePath.getParent());
            Files.writeString(storePath, store.serializeToJson());
            log.info("向量存储已保存: notebook_{}", notebookId);
        } catch (IOException e) {
            log.error("保存向量存储失败: notebook_{}", notebookId, e);
        }
    }

    /**
     * 移除指定笔记本的向量存储（删除笔记本时调用）
     */
    public void removeStore(Long notebookId) {
        stores.remove(notebookId);
        try {
            Files.deleteIfExists(getStorePath(notebookId));
        } catch (IOException e) {
            log.error("删除向量存储文件失败: notebook_{}", notebookId, e);
        }
    }

    /**
     * 应用关闭时，自动保存所有向量存储
     * 
     * @PreDestroy 注解：Spring 在关闭容器时会调用此方法
     * 确保内存中的向量数据不会丢失
     */
    @PreDestroy
    public void persistAll() {
        log.info("应用关闭，开始持久化所有向量存储...");
        stores.forEach((notebookId, store) -> persistStore(notebookId));
        log.info("所有向量存储已持久化完成，共 {} 个", stores.size());
    }

    /**
     * 获取向量存储文件的保存路径
     * 例：./uploads/vector-store/notebook_123.json
     */
    private Path getStorePath(Long notebookId) {
        return Paths.get(uploadDir, "vector-store", "notebook_" + notebookId + ".json");
    }
}
```

**这段代码的核心思路**：
1. `ConcurrentHashMap` 保证多线程安全（多个用户可能同时上传）
2. `computeIfAbsent` 实现"懒加载"——只有需要时才创建/加载
3. `@PreDestroy` 确保数据不会因为服务重启而丢失
4. JSON 序列化是 LangChain4j 原生支持的，一行代码搞定

---

### 3.7 实现来源服务（核心 RAG 处理链路）

> **目标**：实现文件上传后的完整处理链路

**文件位置**：`src/main/java/com/lyhm/airag/service/SourceService.java`（接口）

```java
package com.lyhm.airag.service;

import com.lyhm.airag.model.entity.Source;
import com.lyhm.airag.model.vo.SourceVO;
import com.mybatisflex.core.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 来源服务接口
 */
public interface SourceService extends IService<Source> {

    /**
     * 上传文件来源
     * 包含完整的 RAG 处理链：文件存储 → 解析 → 分块 → 向量化 → 入库
     */
    SourceVO uploadFileSource(MultipartFile file, Long notebookId, Long userId);

    /**
     * 添加文本来源
     */
    SourceVO addTextSource(String title, String content, Long notebookId, Long userId);

    /**
     * 删除来源（同时清理向量存储）
     */
    void deleteSource(Long sourceId, Long notebookId);

    /**
     * 实体转 VO
     */
    SourceVO getSourceVO(Source source);
    List<SourceVO> getSourceVOList(List<Source> sourceList);
}
```

**文件位置**：`src/main/java/com/lyhm/airag/service/impl/SourceServiceImpl.java`（实现）

这是本阶段**最核心的代码**，我们逐段讲解：

```java
package com.lyhm.airag.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.lyhm.airag.config.VectorStoreConfig;
import com.lyhm.airag.exception.BusinessException;
import com.lyhm.airag.exception.ErrorCode;
import com.lyhm.airag.mapper.SourceMapper;
import com.lyhm.airag.model.entity.Notebook;
import com.lyhm.airag.model.entity.Source;
import com.lyhm.airag.model.vo.SourceVO;
import com.lyhm.airag.service.NotebookService;
import com.lyhm.airag.service.SourceService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SourceServiceImpl extends ServiceImpl<SourceMapper, Source>
        implements SourceService {

    @Resource
    private EmbeddingModel embeddingModel;  // DashScope 嵌入模型

    @Resource
    private VectorStoreConfig vectorStoreConfig;  // 向量存储管理器

    @Resource
    private NotebookService notebookService;

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    // ====================== 文件类型白名单 ======================
    private static final List<String> ALLOWED_EXTENSIONS = List.of("pdf", "txt", "md", "docx");
```

**第一部分：文件上传处理**

```java
    @Override
    public SourceVO uploadFileSource(MultipartFile file, Long notebookId, Long userId) {
        // 1. 获取并检查文件扩展名
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, 
                    "不支持的文件类型，仅支持：" + String.join(", ", ALLOWED_EXTENSIONS));
        }

        // 2. 保存文件到磁盘
        //    目录结构：./uploads/sources/{notebookId}/{uuid}.{ext}
        //    使用 UUID 避免文件名冲突
        String savedFileName = UUID.randomUUID() + "." + extension;
        Path savePath = Paths.get(uploadDir, "sources", String.valueOf(notebookId), savedFileName);
        try {
            Files.createDirectories(savePath.getParent());
            file.transferTo(savePath.toFile());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件保存失败");
        }

        // 3. 创建数据库记录（状态：processing）
        Source source = Source.builder()
                .notebookId(notebookId)
                .userId(userId)
                .fileName(originalFilename)
                .fileType(extension)
                .fileSize(file.getSize())
                .filePath(savePath.toString())
                .status("processing")  // 先标记为处理中
                .build();
        save(source);  // 存入数据库

        // 4. 异步处理文档（解析 → 分块 → 向量化）
        try {
            processDocument(source, savePath);
        } catch (Exception e) {
            // 处理失败，更新状态
            source.setStatus("failed");
            source.setErrorMessage(e.getMessage());
            updateById(source);
            log.error("文档处理失败: {}", originalFilename, e);
        }

        // 5. 更新笔记本的来源计数
        updateNotebookSourceCount(notebookId);

        return getSourceVO(source);
    }
```

**第二部分：文档解析和向量化（RAG 核心！）**

```java
    /**
     * 处理文档：解析 → 分块 → 向量化 → 存入向量存储
     * 
     * 这是 RAG 的核心链路！
     */
    private void processDocument(Source source, Path filePath) throws Exception {
        // 步骤 1：用 Apache Tika 解析文档
        // Tika 可以自动识别文件类型，支持 PDF/DOCX/TXT/MD 等
        ApacheTikaDocumentParser parser = new ApacheTikaDocumentParser();
        Document document;
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            document = parser.parse(inputStream);
        }
        
        // 在文档上附加元数据（来源ID），方便后续过滤
        document.metadata().put("sourceId", String.valueOf(source.getId()));

        // 步骤 2：执行分块和向量化
        processDocumentSegments(source, document);
    }

    /**
     * 将文档分块并向量化存储
     */
    private void processDocumentSegments(Source source, Document document) {
        // 步骤 2：文本分块
        // 参数说明：maxSegmentSize=500字符, maxOverlap=50字符
        // maxOverlap=50 表示相邻块之间有 50 字符的重叠，避免在切割处丢失语义
        DocumentSplitter splitter = DocumentSplitters.recursive(500, 50);
        List<TextSegment> segments = splitter.split(document);

        if (segments.isEmpty()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文档内容为空");
        }

        // 为每个分块添加 sourceId 元数据
        segments.forEach(segment -> segment.metadata().put("sourceId", String.valueOf(source.getId())));

        // 步骤 3：向量化
        // 调用 DashScope API 将每个文本块转为向量
        List<Embedding> embeddings = embeddingModel.embedAll(
                segments.stream().map(TextSegment::text).collect(Collectors.toList())
        ).content();

        // 步骤 4：存入向量存储
        InMemoryEmbeddingStore<TextSegment> store = vectorStoreConfig.getOrCreateStore(source.getNotebookId());
        store.addAll(embeddings, segments);

        // 步骤 5：更新数据库中的分块数和状态
        source.setSegmentCount(segments.size());
        source.setStatus("completed");
        updateById(source);

        // 步骤 6：持久化向量存储到 JSON 文件
        vectorStoreConfig.persistStore(source.getNotebookId());

        log.info("文档处理完成: {} → {} 个分块", source.getFileName(), segments.size());
    }
```

**关键理解**：
- `DocumentSplitters.recursive(500, 50)` — 把文档切成每块最多 500 字符，相邻块重叠 50 字符
- `embeddingModel.embedAll()` — 批量调用 DashScope API 将文本转为向量
- `store.addAll(embeddings, segments)` — 把向量和对应的文本块一起存入向量存储
- `vectorStoreConfig.persistStore()` — 把向量存储序列化为 JSON 文件保存

**第三部分：文本来源和删除**

```java
    @Override
    public SourceVO addTextSource(String title, String content, Long notebookId, Long userId) {
        // 文本来源直接创建 Document 对象，跳过文件上传步骤
        Source source = Source.builder()
                .notebookId(notebookId)
                .userId(userId)
                .fileName(title)
                .fileType("text")
                .fileSize((long) content.length())
                .content(content)
                .status("processing")
                .build();
        save(source);

        try {
            // 把文本内容包装成 Document 对象
            Document document = Document.from(content,
                    Metadata.from("sourceId", String.valueOf(source.getId())));
            processDocumentSegments(source, document);
        } catch (Exception e) {
            source.setStatus("failed");
            source.setErrorMessage(e.getMessage());
            updateById(source);
        }

        updateNotebookSourceCount(notebookId);
        return getSourceVO(source);
    }

    @Override
    public void deleteSource(Long sourceId, Long notebookId) {
        // 1. 逻辑删除数据库记录
        removeById(sourceId);

        // 2. 重建向量存储
        // 因为 InMemoryEmbeddingStore 不支持"按条件删除"，
        // 所以删除来源后需要重新处理所有剩余来源
        rebuildVectorStore(notebookId);

        // 3. 更新来源计数
        updateNotebookSourceCount(notebookId);
    }

    /**
     * 重建笔记本的向量存储
     * 
     * 策略：清空旧存储 → 重新处理所有剩余来源
     * 这是 InMemoryEmbeddingStore 的一个局限性
     * 生产环境中使用 Milvus 等向量数据库可以按条件删除
     */
    private void rebuildVectorStore(Long notebookId) {
        // 创建全新的空存储替换旧的
        InMemoryEmbeddingStore<TextSegment> newStore = new InMemoryEmbeddingStore<>();
        // 这里需要手动替换 stores map 中的引用
        // 简化处理：直接获取并清空
        InMemoryEmbeddingStore<TextSegment> store = vectorStoreConfig.getOrCreateStore(notebookId);
        
        // 查询所有未删除的来源，重新处理
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("notebookId", notebookId);
        List<Source> remainingSources = list(queryWrapper);
        
        for (Source source : remainingSources) {
            try {
                if ("text".equals(source.getFileType()) && source.getContent() != null) {
                    Document doc = Document.from(source.getContent(),
                            Metadata.from("sourceId", String.valueOf(source.getId())));
                    processDocumentSegments(source, doc);
                } else if (source.getFilePath() != null) {
                    processDocument(source, Paths.get(source.getFilePath()));
                }
            } catch (Exception e) {
                log.error("重建向量存储时处理来源失败: {}", source.getFileName(), e);
            }
        }
    }

    // VO 转换 和 来源计数更新
    @Override
    public SourceVO getSourceVO(Source source) {
        if (source == null) return null;
        SourceVO vo = new SourceVO();
        BeanUtil.copyProperties(source, vo);
        return vo;
    }

    @Override
    public List<SourceVO> getSourceVOList(List<Source> sourceList) {
        return sourceList.stream().map(this::getSourceVO).collect(Collectors.toList());
    }

    private void updateNotebookSourceCount(Long notebookId) {
        long count = count(QueryWrapper.create().eq("notebookId", notebookId));
        Notebook notebook = notebookService.getById(notebookId);
        if (notebook != null) {
            notebook.setSourceCount((int) count);
            notebookService.updateById(notebook);
        }
    }
}
```

---

### 3.8 创建来源控制器

> **目标**：提供 REST API 端点

**文件位置**：`src/main/java/com/lyhm/airag/controller/SourceController.java`

```java
package com.lyhm.airag.controller;

import com.lyhm.airag.common.BaseResponse;
import com.lyhm.airag.common.DeleteRequest;
import com.lyhm.airag.common.ResultUtils;
import com.lyhm.airag.exception.BusinessException;
import com.lyhm.airag.exception.ErrorCode;
import com.lyhm.airag.exception.ThrowUtils;
import com.lyhm.airag.model.dto.source.SourceTextAddRequest;
import com.lyhm.airag.model.entity.Notebook;
import com.lyhm.airag.model.entity.Source;
import com.lyhm.airag.model.entity.User;
import com.lyhm.airag.model.vo.SourceVO;
import com.lyhm.airag.service.NotebookService;
import com.lyhm.airag.service.SourceService;
import com.lyhm.airag.service.UserService;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/source")
public class SourceController {

    @Resource
    private SourceService sourceService;
    @Resource
    private NotebookService notebookService;
    @Resource
    private UserService userService;

    /**
     * 上传文件来源
     * 
     * 前端通过 FormData 上传：
     *   const formData = new FormData()
     *   formData.append('file', file)
     *   formData.append('notebookId', '123')
     */
    @PostMapping("/upload")
    public BaseResponse<SourceVO> uploadSource(
            @RequestParam("file") MultipartFile file,
            @RequestParam("notebookId") Long notebookId,
            HttpServletRequest request) {
        
        // 参数校验
        ThrowUtils.throwIf(file == null || file.isEmpty(), 
                ErrorCode.PARAMS_ERROR, "请选择要上传的文件");
        ThrowUtils.throwIf(notebookId == null || notebookId <= 0, 
                ErrorCode.PARAMS_ERROR);

        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 校验笔记本归属（确保只能操作自己的笔记本）
        checkNotebookOwnership(notebookId, loginUser.getId());

        // 执行上传和 RAG 处理
        SourceVO sourceVO = sourceService.uploadFileSource(file, notebookId, loginUser.getId());
        return ResultUtils.success(sourceVO);
    }

    /**
     * 添加文本来源
     */
    @PostMapping("/add/text")
    public BaseResponse<SourceVO> addTextSource(
            @RequestBody SourceTextAddRequest sourceTextAddRequest,
            HttpServletRequest request) {
        // ... 参数校验 + 权限校验（同上格式）
        User loginUser = userService.getLoginUser(request);
        checkNotebookOwnership(sourceTextAddRequest.getNotebookId(), loginUser.getId());
        
        SourceVO sourceVO = sourceService.addTextSource(
                sourceTextAddRequest.getTitle(),
                sourceTextAddRequest.getContent(),
                sourceTextAddRequest.getNotebookId(),
                loginUser.getId()
        );
        return ResultUtils.success(sourceVO);
    }

    /**
     * 获取来源列表
     */
    @GetMapping("/list")
    public BaseResponse<List<SourceVO>> listSources(
            @RequestParam Long notebookId,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        checkNotebookOwnership(notebookId, loginUser.getId());

        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("notebookId", notebookId)
                .orderBy("createTime", false);  // 按创建时间倒序
        List<Source> sourceList = sourceService.list(queryWrapper);
        return ResultUtils.success(sourceService.getSourceVOList(sourceList));
    }

    /**
     * 删除来源
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteSource(
            @RequestBody DeleteRequest deleteRequest,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Source source = sourceService.getById(deleteRequest.getId());
        ThrowUtils.throwIf(source == null, ErrorCode.NOT_FOUND_ERROR, "来源不存在");
        ThrowUtils.throwIf(!source.getUserId().equals(loginUser.getId()),
                ErrorCode.NO_AUTH_ERROR, "无权删除该来源");

        sourceService.deleteSource(deleteRequest.getId(), source.getNotebookId());
        return ResultUtils.success(true);
    }

    /**
     * 校验笔记本归属权 — 所有接口都要调用
     * 确保用户只能操作自己的笔记本中的来源
     */
    private void checkNotebookOwnership(Long notebookId, Long userId) {
        Notebook notebook = notebookService.getById(notebookId);
        ThrowUtils.throwIf(notebook == null, ErrorCode.NOT_FOUND_ERROR, "笔记本不存在");
        ThrowUtils.throwIf(!notebook.getUserId().equals(userId),
                ErrorCode.NO_AUTH_ERROR, "无权访问该笔记本");
    }
}
```

---

### 3.9 前端实现

#### 3.9.1 sourceController.ts — API 函数

**文件位置**：`lyhm-ai-rag-frontend/src/api/sourceController.ts`

```typescript
import request from '@/request'

/**
 * 上传文件来源
 * 
 * 关键点：使用 FormData，不设置 Content-Type
 * 让浏览器自动设置 multipart/form-data 以及 boundary
 */
export async function uploadSource(notebookId: number, file: File) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('notebookId', String(notebookId))
  return request<API.BaseResponse<API.SourceVO>>('/source/upload', {
    method: 'POST',
    data: formData,
    // 注意：不要手动设置 Content-Type！
  })
}

/** 添加文本来源 */
export async function addTextSource(body: API.SourceTextAddRequest) {
  return request<API.BaseResponse<API.SourceVO>>('/source/add/text', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
  })
}

/** 获取来源列表 */
export async function listSources(notebookId: number) {
  return request<API.BaseResponse<API.SourceVO[]>>('/source/list', {
    method: 'GET',
    params: { notebookId },
  })
}

/** 删除来源 */
export async function deleteSource(body: API.DeleteRequest) {
  return request<API.BaseResponse<boolean>>('/source/delete', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    data: body,
  })
}
```

#### 3.9.2 NotebookDetailPage.vue — 三栏布局

**文件位置**：`lyhm-ai-rag-frontend/src/pages/notebook/NotebookDetailPage.vue`

```html
<template>
  <div class="notebook-detail-page" v-if="!loading">
    <!-- 顶部标题栏 -->
    <div class="page-header">
      <div class="header-left">
        <a-button type="text" @click="goBack">
          <template #icon><ArrowLeftOutlined /></template>
        </a-button>
        <h2 class="notebook-title">{{ notebook.title || '加载中...' }}</h2>
      </div>
      <div class="header-right">
        <span class="source-count">{{ notebook.sourceCount || 0 }} 个来源</span>
      </div>
    </div>

    <!-- 核心：三栏布局 -->
    <div class="three-column-layout">
      <!-- 左栏(280px)：来源管理 -->
      <div class="left-panel">
        <SourcePanel
          :notebook-id="notebookId"
          @selection-change="onSourceSelectionChange"
          @source-count-change="onSourceCountChange"
        />
      </div>

      <!-- 中栏(自适应)：AI 对话 -->
      <div class="center-panel">
        <ChatPanel
          :notebook-id="notebookId"
          :selected-source-ids="selectedSourceIds"
        />
      </div>

      <!-- 右栏(300px)：Studio（报告+测验） -->
      <div class="right-panel">
        <StudioPanel
          :notebook-id="notebookId"
          :selected-source-ids="selectedSourceIds"
        />
      </div>
    </div>
  </div>
</template>
```

**CSS 三栏布局的关键**：

```css
.three-column-layout {
  display: flex;       /* 横向排列 */
  flex: 1;             /* 占满剩余高度 */
  overflow: hidden;    /* 防止内容溢出 */
}

.left-panel {
  width: 280px;        /* 固定宽度 */
  min-width: 280px;
  border-right: 1px solid #e8e8e8;
  overflow-y: auto;    /* 内容多时可滚动 */
}

.center-panel {
  flex: 1;             /* 自适应剩余宽度 */
  overflow: hidden;
}

.right-panel {
  width: 300px;
  min-width: 300px;
  border-left: 1px solid #e8e8e8;
  overflow-y: auto;
}
```

#### 3.9.3 SourcePanel.vue 和 SourceUploadDialog.vue

来源面板和上传弹窗的核心代码请参考项目中的完整文件，关键设计点：

**SourcePanel.vue 核心功能**：
- 加载来源列表：调用 `listSources(notebookId)`
- 勾选管理：`Set<number>` 存储选中的来源 ID，支持单选/全选
- 删除操作：先通过 Popconfirm 确认，再调用 `deleteSource()`
- 文件图标：根据 fileType 显示不同图标（PDF/Word/Markdown/Text）

**SourceUploadDialog.vue 核心功能**：
- 双 Tab 切换：文件上传 / 文本粘贴
- 文件校验：类型白名单 + 20MB 大小限制
- 拖拽上传：使用 Ant Design 的 `a-upload-dragger` 组件

---

## 小结

本阶段实现了 RAG 知识库最核心的数据入库流程：

```
用户上传文件
    ↓
SourceController 接收 MultipartFile
    ↓
SourceServiceImpl.uploadFileSource()
    ↓
1. 保存文件到磁盘 (./uploads/sources/{notebookId}/{uuid}.ext)
    ↓
2. 创建 Source 数据库记录 (status=processing)
    ↓
3. ApacheTikaDocumentParser 解析文件为纯文本
    ↓
4. DocumentSplitters.recursive(500, 50) 切分为小块
    ↓
5. EmbeddingModel.embedAll() 将每个块转为向量
    ↓
6. InMemoryEmbeddingStore.addAll() 存入向量存储
    ↓
7. 更新 Source.status = "completed"
    ↓
8. VectorStoreConfig.persistStore() 持久化为 JSON 文件
```

学完本阶段，你应该理解了：
- RAG 的文档入库流程
- LangChain4j 的文档解析、分块、嵌入 API
- InMemoryEmbeddingStore 的使用和持久化
- Spring Boot 文件上传的处理方式
