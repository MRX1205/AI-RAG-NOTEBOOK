# API 接口设计文档

## 1. 接口规范

### 1.1 基础约定

| 项目 | 说明 |
|------|------|
| 基础路径 | `/api` |
| 数据格式 | JSON |
| 认证方式 | Session（Cookie） |
| 编码 | UTF-8 |
| 时间格式 | `yyyy-MM-dd HH:mm:ss` |

### 1.2 统一响应格式

```json
{
  "code": 0,        // 0 表示成功，非 0 表示错误
  "data": {},       // 响应数据
  "message": ""     // 错误时的提示信息
}
```

### 1.3 错误码清单

| 错误码 | 含义 |
|--------|------|
| 0 | 成功 |
| 40000 | 请求参数错误 |
| 40100 | 未登录 |
| 40101 | 无权限 |
| 40400 | 数据不存在 |
| 50000 | 系统内部错误 |
| 50010 | AI 服务调用失败 |

---

## 2. 用户模块 API（已有）

> 以下接口已经存在，列出仅供参考。

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/user/register` | 用户注册 |
| POST | `/user/login` | 用户登录 |
| POST | `/user/logout` | 用户注销 |
| GET | `/user/get/login` | 获取当前登录用户信息 |
| POST | `/user/add` | 添加用户（管理员） |
| GET | `/user/get` | 按 ID 获取用户（管理员） |
| POST | `/user/delete` | 删除用户（管理员） |
| POST | `/user/update` | 更新用户（管理员） |
| POST | `/user/list/page/vo` | 分页查询用户列表（管理员） |

---

## 3. 笔记本模块 API（新增）

### 3.1 创建笔记本

```
POST /notebook/add
```

**请求体**：
```json
{
  "title": "基于LangChain4j的RAG系统",
  "description": "研究 LangChain4j 框架的 RAG 实现方案"
}
```

**响应**：
```json
{
  "code": 0,
  "data": 1234567890123456789,  // 笔记本 ID
  "message": ""
}
```

### 3.2 获取笔记本列表

```
GET /notebook/list
```

**响应**：
```json
{
  "code": 0,
  "data": [
    {
      "id": 1234567890123456789,
      "title": "基于LangChain4j的RAG系统",
      "description": "研究 LangChain4j 框架",
      "sourceCount": 3,
      "coverImage": null,
      "createTime": "2026-03-05 22:00:00"
    }
  ]
}
```

### 3.3 获取笔记本详情

```
GET /notebook/get?id={notebookId}
```

### 3.4 更新笔记本

```
POST /notebook/update
```

**请求体**：
```json
{
  "id": 1234567890123456789,
  "title": "新标题",
  "description": "新描述"
}
```

### 3.5 删除笔记本

```
POST /notebook/delete
```

**请求体**：
```json
{
  "id": 1234567890123456789
}
```

---

## 4. 来源管理模块 API（新增）

### 4.1 上传文件来源

```
POST /source/upload
Content-Type: multipart/form-data
```

**请求参数**：

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| file | MultipartFile | 是 | 上传的文件 |
| notebookId | Long | 是 | 所属笔记本 ID |

**响应**：
```json
{
  "code": 0,
  "data": {
    "id": 1234567890123456789,
    "fileName": "LangChain4j入门.pdf",
    "fileType": "pdf",
    "status": "processing"
  }
}
```

### 4.2 添加文本来源

```
POST /source/add/text
```

**请求体**：
```json
{
  "notebookId": 1234567890123456789,
  "title": "我的笔记",
  "content": "这是一段手动输入的文本内容..."
}
```

### 4.3 获取来源列表

```
GET /source/list?notebookId={notebookId}
```

**响应**：
```json
{
  "code": 0,
  "data": [
    {
      "id": 1234567890123456789,
      "fileName": "LangChain4j入门.pdf",
      "fileType": "pdf",
      "fileSize": 1024000,
      "segmentCount": 42,
      "status": "completed",
      "createTime": "2026-03-05 22:00:00"
    }
  ]
}
```

### 4.4 删除来源

```
POST /source/delete
```

**请求体**：
```json
{
  "id": 1234567890123456789
}
```

---

## 5. AI 对话模块 API（新增）

### 5.1 发送消息（流式）

```
GET /chat/stream?notebookId={notebookId}&message={message}&sourceIds={1,2,3}
Accept: text/event-stream
```

**说明**：
- 使用 **SSE（Server-Sent Events）** 协议
- 后端使用 `SseEmitter` 或 `Flux<String>` 返回流式数据
- 每个事件包含一个文本片段

**SSE 流数据格式**：
```
data: 根据
data: 您的
data: 资料
data: ，
data: RAG
data: 是
data: ...
data: [DONE]
```

### 5.2 获取推荐问题

```
GET /chat/suggestions?notebookId={notebookId}
```

**响应**：
```json
{
  "code": 0,
  "data": [
    "LangChain4j 支持哪些向量数据库？",
    "如何实现文档分块策略？",
    "RAG 系统的重排序原理是什么？"
  ]
}
```

---

## 6. ⭐ 报告模块 API（新增 — 重点）

### 6.1 生成报告（流式）

```
POST /report/generate
Accept: text/event-stream
```

**请求体**：
```json
{
  "notebookId": 1234567890123456789,
  "reportType": "briefing",
  "sourceIds": [1, 2, 3],
  "customPrompt": null
}
```

**说明**：
- `reportType` 可选值：`briefing`、`study_guide`、`faq`、`timeline`、`custom`
- 当 `reportType` 为 `custom` 时，`customPrompt` 必填
- 返回 SSE 流式数据（Markdown 格式文本片段）
- 流结束后自动保存报告到数据库

**SSE 流数据格式**：
```
data: # 
data: 简报
data: ：
data: LangChain4j
data:  RAG 
data: 系统
data: 
data: ## 摘要
data: ...
data: {"reportId": 123456}
data: [DONE]
```

> 在流结束前，发送一个包含 `reportId` 的 JSON 事件，前端据此跳转到报告详情。

### 6.2 获取报告列表

```
GET /report/list?notebookId={notebookId}
```

**响应**：
```json
{
  "code": 0,
  "data": [
    {
      "id": 1234567890123456789,
      "title": "简报：LangChain4j RAG 系统",
      "reportType": "briefing",
      "createTime": "2026-03-05 22:30:00"
    }
  ]
}
```

### 6.3 获取报告详情

```
GET /report/get?id={reportId}
```

**响应**：
```json
{
  "code": 0,
  "data": {
    "id": 1234567890123456789,
    "title": "简报：LangChain4j RAG 系统",
    "reportType": "briefing",
    "content": "# 简报：LangChain4j RAG 系统\n\n## 摘要\n...",
    "sourceIds": "1,2,3",
    "createTime": "2026-03-05 22:30:00"
  }
}
```

### 6.4 删除报告

```
POST /report/delete
```

**请求体**：
```json
{
  "id": 1234567890123456789
}
```

---

## 7. ⭐ 测验模块 API（新增 — 重点）

### 7.1 生成测验

```
POST /quiz/generate
```

**请求体**：
```json
{
  "notebookId": 1234567890123456789,
  "questionCount": 10,
  "difficulty": "medium",
  "sourceIds": [1, 2, 3]
}
```

**响应**：
```json
{
  "code": 0,
  "data": {
    "id": 1234567890123456789,
    "title": "知识测验 - 2026-03-05",
    "questionCount": 10,
    "difficulty": "medium",
    "questions": [
      {
        "questionId": 1,
        "question": "RAG 系统中 Retrieval 的含义是什么？",
        "options": [
          {"label": "A", "text": "数据检索"},
          {"label": "B", "text": "文本生成"},
          {"label": "C", "text": "模型训练"},
          {"label": "D", "text": "数据清洗"}
        ]
      }
    ]
  }
}
```

> **注意**：生成时返回的 `questions` 中 **不包含** `correctAnswer` 和 `explanation` 字段，防止前端作弊。

### 7.2 提交测验答案

```
POST /quiz/submit
```

**请求体**：
```json
{
  "quizId": 1234567890123456789,
  "answers": [
    {"questionId": 1, "answer": "A"},
    {"questionId": 2, "answer": "C"},
    {"questionId": 3, "answer": "B"}
  ],
  "timeCost": 180
}
```

**响应**：
```json
{
  "code": 0,
  "data": {
    "recordId": 1234567890123456789,
    "score": 70,
    "correctCount": 7,
    "totalCount": 10,
    "results": [
      {
        "questionId": 1,
        "userAnswer": "A",
        "correctAnswer": "A",
        "isCorrect": true,
        "explanation": "RAG 中的 R 代表 Retrieval（检索）..."
      },
      {
        "questionId": 2,
        "userAnswer": "C",
        "correctAnswer": "B",
        "isCorrect": false,
        "explanation": "正确答案是 B，因为..."
      }
    ]
  }
}
```

> **关键设计**：正确答案和解释仅在提交答案后返回，保证答题公平性。

### 7.3 获取测验列表

```
GET /quiz/list?notebookId={notebookId}
```

### 7.4 获取测验详情

```
GET /quiz/get?id={quizId}
```

> 获取详情时包含完整的 `questions`（含正确答案和解释），仅对测验拥有者可见。

### 7.5 获取答题记录列表

```
GET /quiz/records?quizId={quizId}
```

### 7.6 删除测验

```
POST /quiz/delete
```

---

## 8. 接口汇总表

| 模块 | 方法 | 路径 | 说明 | 状态 |
|------|------|------|------|------|
| **笔记本** | POST | `/notebook/add` | 创建笔记本 | 🆕 |
| | GET | `/notebook/list` | 获取笔记本列表 | 🆕 |
| | GET | `/notebook/get` | 获取笔记本详情 | 🆕 |
| | POST | `/notebook/update` | 更新笔记本 | 🆕 |
| | POST | `/notebook/delete` | 删除笔记本 | 🆕 |
| **来源** | POST | `/source/upload` | 上传文件来源 | 🆕 |
| | POST | `/source/add/text` | 添加文本来源 | 🆕 |
| | GET | `/source/list` | 获取来源列表 | 🆕 |
| | POST | `/source/delete` | 删除来源 | 🆕 |
| **对话** | GET | `/chat/stream` | AI 对话（SSE 流式） | 🆕 |
| | GET | `/chat/suggestions` | 获取推荐问题 | 🆕 |
| **报告** | POST | `/report/generate` | 生成报告（SSE 流式） | 🆕 |
| | GET | `/report/list` | 获取报告列表 | 🆕 |
| | GET | `/report/get` | 获取报告详情 | 🆕 |
| | POST | `/report/delete` | 删除报告 | 🆕 |
| **测验** | POST | `/quiz/generate` | 生成测验 | 🆕 |
| | POST | `/quiz/submit` | 提交测验答案 | 🆕 |
| | GET | `/quiz/list` | 获取测验列表 | 🆕 |
| | GET | `/quiz/get` | 获取测验详情 | 🆕 |
| | GET | `/quiz/records` | 获取答题记录 | 🆕 |
| | POST | `/quiz/delete` | 删除测验 | 🆕 |
