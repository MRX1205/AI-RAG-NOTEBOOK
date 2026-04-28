# AI RAG Notebook — 基于 LangChain4j 的 RAG 知识库问答系统

> 毕业设计项目 · Graduation Design Project

一款受 Google NotebookLM 启发、使用 **RAG（检索增强生成）** 技术构建的私有知识库问答平台。用户可以将 PDF、Word、TXT 等文档上传至个人"笔记本"，然后通过 AI 对话基于文档内容进行问答、生成报告和测验，实现对私有知识库的高效管理与利用。

---

## ✨ 功能特性

- **📚 笔记本管理** — 创建、编辑、删除个人知识笔记本，支持封面图片上传
- **📄 多格式来源上传** — 支持 PDF、DOCX、TXT、Markdown 等格式，自动完成文档解析 → 分块 → 向量化全链路处理
- **💬 RAG 智能对话** — 基于笔记本内容进行精准问答，支持 SSE 流式输出，回答自动标注引用来源
- **📝 AI 报告生成** — 一键生成简报（Briefing）、学习指南（Study Guide）、FAQ、时间线（Timeline）等多种类型报告
- **🧠 AI 测验生成** — 根据知识库内容自动出题，支持难度分级，记录答题历史
- **⭐ 精选笔记本** — 管理员可设置精选笔记本展示，用户可一键克隆精选笔记本
- **👤 用户系统** — 注册/登录、个人信息管理、已保存笔记管理
- **🛡️ 管理后台** — 用户管理、笔记本管理、数据统计仪表盘
- **📖 API 文档** — 集成 Knife4j，提供在线 Swagger UI 文档

---

## 🛠️ 技术栈

### 后端

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 21 | 后端语言 |
| Spring Boot | 3.x | 应用框架 |
| LangChain4j | 1.1.0 | AI/LLM 编排框架 |
| DeepSeek | — | 对话大模型（OpenAI 兼容接口） |
| Alibaba DashScope | — | 向量嵌入模型（text-embedding-v3） |
| MyBatis-Flex | 1.11.0 | ORM 框架 |
| MySQL | 8.x | 关系型数据库 |
| Apache Tika | — | 文档解析（PDF/DOCX/TXT） |
| Spring WebFlux | — | SSE 流式响应 |
| Knife4j | 4.4.0 | API 文档 |

### 前端

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue 3 | 3.5.x | 前端框架 |
| TypeScript | 5.8.x | 类型安全 |
| Vite | 7.x | 构建工具 |
| Ant Design Vue | 4.x | UI 组件库 |
| Vue Router | 4.x | 路由管理 |
| Pinia | 3.x | 状态管理 |
| Axios | 1.x | HTTP 客户端 |

---

## 📸 项目截图

<table>
  <tr>
    <td><img src="pic/截屏2026-03-05 21.46.16.png" alt="首页" width="400"/></td>
    <td><img src="pic/截屏2026-03-05 22.11.52.png" alt="笔记本详情" width="400"/></td>
  </tr>
  <tr>
    <td><img src="pic/截屏2026-03-05 22.12.06.png" alt="AI 对话" width="400"/></td>
    <td><img src="pic/截屏2026-03-05 22.13.06.png" alt="报告生成" width="400"/></td>
  </tr>
</table>

---

## 🚀 本地运行指南

### 环境要求

| 工具 | 版本要求 |
|------|----------|
| JDK | 21+ |
| Maven | 3.9+ |
| Node.js | 18+ |
| npm | 9+ |
| MySQL | 8.0+ |

### 1. 克隆项目

```bash
git clone https://github.com/MRX1205/AI-RAG-NOTEBOOK.git
cd AI-RAG-NOTEBOOK
```

### 2. 初始化数据库

登录 MySQL，按顺序执行以下 SQL 脚本：

```sql
-- 第一步：创建数据库和用户表
source sql/user.sql

-- 第二步：创建笔记本及核心业务表
source sql/notebook.sql

-- 第三步（可选）：执行增量迁移脚本
source sql/migration_v2.sql
source sql/migration_v3.sql
source sql/migration_v4.sql
```

### 3. 配置后端

编辑 `src/main/resources/application-local.yml`，填入你的 API Key：

```yaml
# AI 大模型配置（DeepSeek，兼容 OpenAI 接口）
langchain4j:
  open-ai:
    chat-model:
      base-url: https://api.deepseek.com
      api-key: 你的 DeepSeek API Key       # 填入你的 API Key
      model-name: deepseek-chat
    streaming-chat-model:
      base-url: https://api.deepseek.com
      api-key: 你的 DeepSeek API Key       # 填入你的 API Key
      model-name: deepseek-chat
      max-tokens: 8192

# 阿里云百炼 DashScope Embedding 模型配置
dashscope:
  api-key: 你的 DashScope API Key          # 填入你的 API Key
  embedding:
    model-name: text-embedding-v3
```

> **API Key 获取：**
> - DeepSeek API Key：前往 [DeepSeek 开放平台](https://platform.deepseek.com/) 注册获取
> - DashScope API Key：前往 [阿里云百炼控制台](https://bailian.console.aliyun.com/) 注册获取

如需修改数据库连接，编辑 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/lyhm_ai_rag
    username: root
    password: 123456      # 修改为你的数据库密码
```

### 4. 启动后端

```bash
# 使用 Maven Wrapper（推荐）
./mvnw spring-boot:run

# 或使用本地 Maven
mvn spring-boot:run
```

后端启动后访问：
- **API 服务**：`http://localhost:8123/api`
- **接口文档（Knife4j）**：`http://localhost:8123/api/doc.html`

### 5. 安装前端依赖

```bash
cd lyhm-ai-rag-frontend
npm install
```

### 6. 启动前端开发服务器

```bash
npm run dev
```

前端启动后访问：`http://localhost:5173`

> 前端已配置代理，所有 `/api` 请求会自动转发至后端 `http://localhost:8123`，无需额外配置跨域。

---

## 🗂️ 项目结构

```
AI-RAG-NOTEBOOK/
├── src/main/java/com/lyhm/airag/
│   ├── ai/                     # AI/RAG 核心服务（对话、检索）
│   ├── config/                 # 配置类（向量存储、跨域、嵌入模型等）
│   ├── controller/             # REST API 控制器
│   ├── service/                # 业务逻辑层
│   │   └── impl/               # 业务实现
│   ├── mapper/                 # MyBatis-Flex Mapper
│   ├── model/                  # 数据模型（entity/dto/vo）
│   ├── aop/                    # AOP 切面（权限校验）
│   ├── annotation/             # 自定义注解
│   ├── exception/              # 全局异常处理
│   └── common/                 # 公共类（分页、响应封装等）
├── src/main/resources/
│   ├── application.yml         # 主配置文件
│   ├── application-local.yml   # 本地环境配置（API Key 等敏感信息）
│   └── prompt/                 # AI Prompt 模板
├── lyhm-ai-rag-frontend/       # Vue 3 前端项目
│   └── src/
│       ├── pages/              # 页面组件
│       ├── api/                # API 请求封装
│       ├── stores/             # Pinia 状态管理
│       ├── router/             # Vue Router 路由配置
│       └── components/         # 公共组件
├── sql/                        # 数据库 SQL 脚本
├── uploads/                    # 文件上传目录（运行时自动创建）
└── pic/                        # 项目截图
```

---

## 📦 生产构建

### 构建后端 JAR

```bash
./mvnw clean package -DskipTests
java -jar target/ai-rag-0.0.1-SNAPSHOT.jar
```

### 构建前端静态资源

```bash
cd lyhm-ai-rag-frontend
npm run build
# 构建产物位于 lyhm-ai-rag-frontend/dist/
```

---

## ⚙️ 配置说明

| 配置项 | 文件 | 说明 |
|--------|------|------|
| `server.port` | `application.yml` | 后端服务端口，默认 `8123` |
| `spring.datasource.*` | `application.yml` | 数据库连接配置 |
| `langchain4j.open-ai.chat-model.api-key` | `application-local.yml` | DeepSeek API Key |
| `dashscope.api-key` | `application-local.yml` | 阿里云 DashScope API Key |
| `file.upload-dir` | `application-local.yml` | 文件上传目录，默认 `./uploads` |
| `app.admin-register-code` | `application.yml` | 管理员注册邀请码，默认 `1205` |
| `spring.servlet.multipart.max-file-size` | `application-local.yml` | 最大文件上传大小，默认 `20MB` |

---

## 📄 License

本项目为毕业设计作品，仅供学习参考。
