# 任务分析文档

> 生成时间：2026-03-06  
> 分析人：AI Agent  
> 目的：对项目现状进行全面评估，识别已完成/待完成任务，制定后续开发优先级

---

## 一、项目当前状态评估

### 1.1 总体结论

**代码层面：五个开发阶段全部完成。**  
**验证层面：编译通过，全流程功能测试尚待本次执行。**

| 维度 | 状态 | 说明 |
|------|------|------|
| 后端代码完整性 | ✅ 完整 | 所有 Controller/Service/Entity/Config 均已创建 |
| 前端代码完整性 | ✅ 完整 | 所有页面组件、API 层、路由均已创建 |
| 数据库设计 | ✅ 完整 | SQL 脚本存在（user.sql / notebook.sql / migration_v2.sql） |
| 编译状态 | ✅ 通过 | 后端 `mvn compile` 成功，前端 `vue-tsc` 无错误 |
| Git 版本管理 | ⚠️ 落后 | 仅有 4 次提交，所有新功能代码未入库（本地未提交状态） |
| 全流程功能测试 | ❓ 待验证 | 文档中有 API 单点测试，但完整的端到端流程未被本次会话验证 |

---

### 1.2 后端代码现状

```
src/main/java/com/lyhm/airag/
├── ai/
│   ├── AiNotebookService.java         ✅ AI 服务接口（@SystemMessage 注解驱动）
│   ├── AiNotebookServiceFactory.java  ✅ AI 服务工厂
│   └── RagService.java                ✅ RAG 核心检索（top-5，minScore=0.5）
│
├── config/
│   ├── EmbeddingConfig.java           ✅ DashScope text-embedding-v3 配置
│   ├── VectorStoreConfig.java         ✅ InMemoryEmbeddingStore + JSON 持久化
│   ├── CorsConfig.java                ✅ 跨域配置
│   └── WebMvcConfig.java              ✅ Web 配置
│
├── controller/
│   ├── NotebookController.java        ✅ 笔记本 CRUD（含级联删除）
│   ├── SourceController.java          ✅ 文件上传 + 来源管理
│   ├── ChatController.java            ✅ SSE 流式对话 + 推荐问题
│   ├── ReportController.java          ✅ 报告 SSE 流式生成 + CRUD
│   └── QuizController.java            ✅ 测验生成 + 自动评分
│
├── service/impl/
│   ├── NotebookServiceImpl.java       ✅ 含 @Transactional 级联删除
│   ├── SourceServiceImpl.java         ✅ 文档解析→分块→向量化完整链路
│   ├── ReportServiceImpl.java         ✅ 报告生成业务逻辑
│   └── QuizServiceImpl.java           ✅ 测验生成 + 成绩计算
│
├── model/entity/
│   ├── Notebook.java / Source.java    ✅
│   ├── Report.java / Quiz.java        ✅
│   └── QuizRecord.java                ✅ ID 策略已修复为 Auto
│
└── resources/
    ├── prompt/core-prompt.txt         ✅ 核心 Prompt 模板
    ├── application-local.yml          ✅ DeepSeek + DashScope 配置完整
    └── (prompt 目录仅有 core-prompt)  ⚠️ 报告/测验专用 Prompt 文件待确认
```

**关键配置信息：**
- 对话模型：DeepSeek API（`deepseek-chat`）
- Embedding 模型：阿里 DashScope `text-embedding-v3`
- 向量存储：`InMemoryEmbeddingStore`（带 JSON 文件持久化）
- 文件上传目录：`./uploads`（相对路径，需确认存在）

---

### 1.3 前端代码现状

```
lyhm-ai-rag-frontend/src/
├── pages/
│   ├── HomePage.vue                   ✅ NotebookLM 风格首页（卡片列表 + 顶部导航）
│   ├── notebook/
│   │   └── NotebookDetailPage.vue     ✅ 三栏布局（300px / flex / 320px）
│   └── user/ + admin/                ✅ 已有页面
│
├── components/notebook/
│   ├── ChatPanel.vue                  ✅ SSE 流式对话 + Markdown 渲染 + 操作按钮
│   ├── SourcePanel.vue                ✅ 来源列表 + 搜索 + 全选 + 上传
│   ├── StudioPanel.vue                ✅ 彩色工具卡片（报告/测验可用，其余标注"即将推出"）
│   ├── SourceUploadDialog.vue         ✅ 文件上传弹窗
│   └── CreateNotebookDialog.vue       ✅ 创建笔记本弹窗
│
├── api/
│   ├── notebookController.ts          ✅
│   ├── sourceController.ts            ✅（SSE URL 已修复为动态）
│   ├── chatController.ts              ✅（SSE URL 已修复为动态）
│   ├── reportController.ts / quizController.ts  ✅
│   └── typings.d.ts                   ✅ 完整类型定义
│
├── request.ts                         ✅ baseURL 改为 `/api`（相对路径）
├── vite.config.ts                     ✅ 已添加 proxy → `localhost:8123`
└── router/index.ts                    ✅ hideLayout 路由元数据配置完整
```

---

## 二、待完成任务清单（按优先级排序）

### P0 — 必须完成（核心功能验证）

| # | 任务 | 说明 | 风险等级 |
|---|------|------|---------|
| 1 | **全流程端到端测试** | 启动三服务→登录→创建笔记本→上传文件→RAG对话→生成报告→生成测验 | 高 |
| 2 | **确认 prompt 目录** | `resources/prompt/` 中只有 `core-prompt.txt`，报告生成使用的 5 种类型 Prompt 是否内嵌在代码中还是读取文件？需核实 ReportServiceImpl | 高 |
| 3 | **向量存储持久化路径** | `./uploads` 相对路径在不同工作目录下可能指向不同位置，需确认向量文件实际存储路径 | 中 |
| 4 | **Git 代码提交** | 所有功能代码均未入库，存在代码丢失风险 | 高 |

### P1 — 重要（功能完整性）

| # | 任务 | 说明 | 风险等级 |
|---|------|------|---------|
| 5 | **SSE 流式对话验证** | 验证前端 EventSource 接收后端 SseEmitter 的完整链路 | 中 |
| 6 | **文件类型白名单验证** | 确认 SourceController 对 PDF/TXT/DOCX/MD 的类型校验是否生效 | 低 |
| 7 | **测验 JSON 解析健壮性** | LLM 返回的 JSON 可能格式不规范，确认后端是否有兜底处理 | 中 |
| 8 | **来源勾选控制 RAG 范围** | 确认 ChatController 接收 sourceIds 参数后 RagService 是否正确过滤向量片段 | 中 |

### P2 — 锦上添花

| # | 任务 | 说明 |
|---|------|------|
| 9 | 报告导出（PDF/Markdown） | 需求文档中标注 P2 |
| 10 | 来源详情预览弹窗 | 需求文档中标注 P2 |
| 11 | 对话历史持久化 | 当前对话是否保存到数据库待确认 |
| 12 | 错误页面/加载骨架屏 | UI 优化项 |

---

## 三、技术方案分析

### 3.1 RAG 架构评估

```
用户上传文件
    ↓
SourceServiceImpl（Apache Tika 解析 → DocumentSplitter 分块 → DashScope Embedding → InMemoryStore）
    ↓（向量持久化为 JSON 文件）

用户提问
    ↓
RagService（从 InMemoryStore 检索 top-5，minScore=0.5）
    ↓
ChatController（组装 Prompt + 调用 DeepSeek 流式输出）
    ↓（SSE）
ChatPanel.vue（EventSource 接收 + Markdown 渲染）
```

**优点**：
- 架构清晰，无外部向量数据库依赖，部署简单
- DashScope 中文 Embedding 效果优于 DeepSeek 聊天模型用于向量化
- SSE 流式输出用户体验好

**潜在风险**：
- `InMemoryEmbeddingStore` 数据全在内存中，重启后需从 JSON 文件重新加载，需确认加载逻辑
- minScore=0.5 的阈值可能导致检索结果过多噪音，或者太严格导致无结果

### 3.2 测验生成的 JSON 格式风险

LLM（DeepSeek）生成 JSON 时可能：
- 在 JSON 前后附加说明文字
- 使用 markdown 代码块包裹（` ```json ... ``` `）
- 生成不完整的 JSON（截断）

需确认 `QuizServiceImpl` 中是否有正则提取 JSON 数组的兜底逻辑。

### 3.3 向量文件按笔记本隔离

根据 `VectorStoreConfig` 设计，每个笔记本应有独立的向量存储文件（如 `./uploads/vectors/notebook_{id}.json`）。这是正确的设计，确保不同笔记本的知识不互相污染。

---

## 四、潜在风险点

| 风险 | 严重程度 | 应对方案 |
|------|---------|---------|
| Git 无备份：所有核心功能代码仅在本地 | 🔴 高 | 立即提交 git |
| Prompt 文件缺失：报告生成 5 种类型 Prompt 需确认位置 | 🟡 中 | 检查 ReportServiceImpl，确认 Prompt 是硬编码还是读文件 |
| InMemoryStore 重启丢失：每次重启需重新加载 JSON | 🟡 中 | 确认 VectorStoreConfig 中的加载/持久化逻辑 |
| API Key 明文存储：DeepSeek + DashScope key 在 yml 文件中 | 🟡 中 | 生产环境改为环境变量；开发阶段可接受 |
| 向量检索无结果：minScore 阈值不当导致空检索 | 🟡 中 | 测试时关注 RAG 检索是否返回内容 |
| 测验 JSON 解析失败：LLM 输出不规范 JSON | 🟡 中 | 验证 QuizServiceImpl 的兜底处理 |

---

## 五、下一步行动计划

### 立即执行（本次会话）

1. **检查关键实现细节**（10 分钟）
   - 阅读 `ReportServiceImpl.java`：确认报告 Prompt 来源
   - 阅读 `QuizServiceImpl.java`：确认 JSON 解析兜底逻辑
   - 阅读 `VectorStoreConfig.java`：确认持久化/加载机制

2. **启动并验证系统**（15 分钟）
   - 启动 MySQL → 后端 → 前端
   - 执行完整功能流程验证
   - 记录测试结果

3. **Git 提交**（5 分钟）
   - 将所有未提交代码入库

### 后续迭代

- 根据测试结果修复发现的 Bug
- 补充缺失的功能（如对话历史持久化）
- UI 细节优化

---

## 六、文档索引

| 文档 | 内容 | 状态 |
|------|------|------|
| `01-项目概述.md` | 项目背景、技术栈、架构图 | ✅ |
| `02-需求分析.md` | 功能需求、非功能需求、用例图 | ✅ |
| `03-模块设计.md` | 包结构、组件设计、状态管理 | ✅ |
| `04-数据库设计.md` | 数据库表结构 | ✅ |
| `05-API接口设计.md` | RESTful API 规范 | ✅ |
| `06-开发计划.md` | 5 阶段开发计划、技术攻关点 | ✅ |
| `07-开发任务规划-阶段二三四.md` | 阶段二~四任务分解与编译记录 | ✅ |
| `阶段报告-阶段五-优化完善.md` | Bug 修复记录、级联删除实现 | ✅ |
| `阶段报告-系统联调与UI优化.md` | Vite 代理配置、UI 全面重设计 | ✅ |
| `修复报告.md` | 系统排查记录、运行前提条件 | ✅ |
| `task-analysis.md`（本文档）| 当前状态评估、待完成任务 | ✅ |
