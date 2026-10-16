# AI-RAG-NOTEBOOK

基于 **LangChain4j + Spring Boot + Vue 3** 的 RAG 知识库问答系统（毕业设计项目）。

[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://www.oracle.com/java/technologies/downloads/#java21)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.11-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.5.17-brightgreen.svg)](https://vuejs.org/)

---

## 项目简介

本项目聚焦“私有资料可溯源问答”，核心流程为：

1. 用户创建笔记本并上传来源（文件或文本）
2. 系统解析内容、切分文本、生成向量并按笔记本持久化
3. 对话/报告/测验等 AI 能力统一基于检索到的上下文生成结果
4. 支持对话保存为笔记、管理后台、公开精选笔记本浏览

后端默认上下文路径为 `/api`，前端通过 `/api` 代理访问后端接口。

---

## 核心功能

### 1) 用户与权限
- 用户注册、登录、登出、个人信息维护
- 角色区分：`user` / `admin`
- 管理员支持用户管理、账号启停、重置密码、系统统计

### 2) 笔记本与来源管理
- 笔记本创建、编辑、删除、封面上传
- 支持来源类型：
  - 文件上传：`pdf / txt / md / docx`
  - 手动文本来源
- 来源处理链路：上传 → 解析（Apache Tika）→ 分块（递归分块）→ Embedding → 入库

### 3) RAG 对话（SSE 流式）
- 基于笔记本上下文实时问答
- 支持按来源 ID 过滤检索
- 流式返回（SSE），前端逐段展示
- 支持推荐问题生成

### 4) 报告生成
- 支持报告类型：`briefing / study_guide / faq / timeline / custom`
- SSE 流式生成，完成后自动入库

### 5) 测验生成与评分
- 按难度生成选择题（`easy / medium / hard`）
- 保存题目 JSON，提交后自动评分并记录答题结果

### 6) Saved Note（对话沉淀）
- 可将 AI 回答保存为笔记
- 支持分页、更新、删除

### 7) 公开精选与克隆
- 公开接口可浏览精选笔记本
- 登录用户可克隆精选笔记本为个人副本

---

## 技术栈

### 后端
- Java 21
- Spring Boot 3.5.11（Web + WebFlux）
- MyBatis-Flex 1.11.0
- LangChain4j（核心 1.1.0，集成 1.1.0-beta7）
- Apache Tika 文档解析（langchain4j document parser）
- MySQL 8.x
- Knife4j / SpringDoc（接口文档）

### 前端
- Vue 3 + TypeScript
- Vite 7
- Vue Router + Pinia
- Ant Design Vue 4
- Axios

---

## 系统架构（简化）

```text
Vue3 前端
   ↓ REST/SSE
Spring Boot 后端
   ├─ User/Notebook/Source/Chat/Report/Quiz/Note/Admin Controller
   ├─ Service + MyBatis-Flex
   ├─ RagService（检索与上下文拼装）
   └─ VectorStoreConfig（按 notebookId 管理向量存储）
        ↓
MySQL（结构化业务数据）
文件系统 uploads/（来源文件、头像、封面、向量 JSON）
LLM/Embedding 服务（DeepSeek + DashScope OpenAI兼容）
```

---

## 目录结构

```text
.
├── src/main/java/com/lyhm/airag
│   ├── controller          # 业务接口层
│   ├── service             # 业务服务层
│   ├── ai                  # RAG/AI 相关服务
│   ├── config              # Embedding/向量存储/CORS 等配置
│   ├── mapper              # MyBatis-Flex Mapper
│   └── model               # DTO/Entity/VO/Enum
├── src/main/resources
│   ├── application.yml
│   └── application-local.yml
├── sql                     # 建表与迁移脚本
├── lyhm-ai-rag-frontend    # 前端工程
├── uploads                 # 本地上传与向量存储目录（运行时生成）
└── langchain4j-examples-main  # LangChain4j 参考示例代码
```

---

## 环境要求

- JDK 21+
- Maven 3.9+（或使用 `./mvnw`）
- Node.js 18+（建议 20+）
- MySQL 8.0+
- 可用的 LLM 与 Embedding Key

---

## 快速开始

## 1. 初始化数据库

建议执行顺序：

1. `sql/user.sql`
2. `sql/notebook.sql`
3. `sql/migration_v2.sql`
4. `sql/migration_v3.sql`
5. `sql/migration_v4.sql`

数据库名默认为：`lyhm_ai_rag`

## 2. 配置后端

编辑：
- `/home/runner/work/AI-RAG-NOTEBOOK/AI-RAG-NOTEBOOK/src/main/resources/application.yml`
- `/home/runner/work/AI-RAG-NOTEBOOK/AI-RAG-NOTEBOOK/src/main/resources/application-local.yml`

重点配置项：
- `spring.datasource.*`（MySQL）
- `langchain4j.open-ai.chat-model.*`（聊天模型）
- `dashscope.api-key` 与 `dashscope.embedding.model-name`（Embedding）
- `file.upload-dir`（上传目录，默认 `./uploads`）

## 3. 启动后端

```bash
cd /home/runner/work/AI-RAG-NOTEBOOK/AI-RAG-NOTEBOOK
./mvnw spring-boot:run
```

默认地址：
- 后端：`http://localhost:8123/api`
- 健康检查：`http://localhost:8123/api/health/`
- API 文档（Knife4j）：`http://localhost:8123/api/doc.html`

## 4. 启动前端

```bash
cd /home/runner/work/AI-RAG-NOTEBOOK/AI-RAG-NOTEBOOK/lyhm-ai-rag-frontend
npm install
npm run dev
```

前端开发服务默认通过 `/api` 调用后端接口（`src/request.ts`）。

---

## 常用开发命令

### 后端
```bash
./mvnw clean test
./mvnw spring-boot:run
```

### 前端
```bash
npm run dev
npm run build
npm run lint
```

---

## 主要接口分组

- `/user`：注册、登录、用户信息、管理员用户管理
- `/notebook`：笔记本 CRUD、封面上传、克隆、管理员管理
- `/source`：来源上传/新增文本/列表/删除
- `/chat`：RAG 对话流、推荐问题
- `/report`：报告生成（SSE）、列表、详情、删除
- `/quiz`：测验生成、提交评分、列表、详情、删除
- `/note`：保存笔记、分页查询、更新、删除
- `/public/notebooks`：公开精选列表与详情
- `/admin`：系统统计

---

## 注意事项

1. 项目使用本地文件系统持久化向量数据（`uploads/vector-store`），适合学习与单机开发。
2. `src/test` 中部分 AI 测试依赖真实模型配置，未配置 Key 时可能失败。
3. 请勿将真实 API Key 写入仓库；建议使用本地环境变量或私有配置覆盖。

---

## License

本项目当前未单独声明开源许可证，请按仓库实际授权策略使用。
