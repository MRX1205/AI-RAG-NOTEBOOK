# 思考文档：修复未完成功能（4个问题）

> 生成时间：2026-03-06  
> 分析人：AI Agent  
> 方法：通读全部源码后逐一定位根本原因

---

## 一、代码探查结论总览

| 问题 | 根本原因 | 严重程度 | 是否新增代码 |
|------|---------|---------|------------|
| 问题1a：保存笔记无法工作 | `SavedNoteController.java` 文件丢失（未写入磁盘）| 🔴 阻断 | 重建文件 |
| 问题1b：简报面板不显示保存笔记 | `StudioPanel.vue` 无笔记展示区，需新增可折叠笔记栏 | 🟡 缺失功能 | 新增 |
| 问题2：首页未登录看不到精选 | 路由守卫已正确（requiresAuth:false），但点击精选卡片直接跳登录页而非弹提示；数据库中暂无精选笔记 | 🟡 UX缺陷 | 少量修改 |
| 问题3：管理员导航入口 | GlobalHeader 已修复（上次开发），但 HomePage 用了独立 top-nav 不走 GlobalHeader；非首页已正确 | 🟡 仅首页 | 少量修改 |
| 问题4a：用户名/头像修改 | UserProfilePage 已实现，但头像预览 URL 缺 `/api` 前缀导致图片无法显示 | 🟡 显示Bug | 1行修复 |
| 问题4b：笔记本名称/封面修改 | NotebookDetailPage 已有内联标题编辑和封面上传，封面URL也有重复拼接`/api`问题 | 🟡 显示Bug | 1行修复 |

---

## 二、问题一详细分析

### 问题1a：SavedNoteController.java 丢失

**现象：** 调用 `POST /api/note/save` 返回 404 Not Found，「保存到笔记」按钮点击后弹出「网络错误，保存失败」

**根本原因：** 通过 `ls` 命令确认，`src/main/java/com/lyhm/airag/controller/` 目录下**不存在** `SavedNoteController.java`。文件列表为：
```
ChatController.java, HealthController.java, NotebookController.java
QuizController.java, ReportController.java, SourceController.java, UserController.java
```
`SavedNoteController.java` 在上次开发中写入失败（文件丢失），导致所有 `/note/*` 接口均 404。

**相关服务层文件均存在（正常）：**
- `SavedNote.java`（Entity）✅
- `SavedNoteMapper.java` ✅
- `SavedNoteService.java` ✅
- `SavedNoteServiceImpl.java` ✅
- `SavedNoteVO.java` ✅
- `model/dto/note/SaveNoteRequest.java` ✅
- `model/dto/note/UpdateNoteRequest.java` ✅

**修复方案：** 重新创建 `SavedNoteController.java`，接口路径：
- `POST /note/save`
- `GET /note/list`
- `PUT /note/{id}`
- `DELETE /note/{id}`

---

### 问题1b：简报面板（笔记展示区）未实现

**现象：** `StudioPanel.vue` 只展示报告（Report）和测验（Quiz），没有 AI 对话中保存的笔记展示区。

**当前 StudioPanel 结构：**
- 工具卡片区（报告、测验入口）
- 已生成报告列表
- 已生成测验列表
- 底部「添加笔记」按钮（仅 UI，无逻辑）

**目标效果（NotebookLM Studio Panel 风格）：**
```
StudioPanel
├── 📋 报告区域（已有）
├── 📝 测验区域（已有）  
└── 📌 已保存笔记区域（需新增）
    ├── [折叠/展开按钮]
    ├── 笔记卡片1：标题 + 前50字摘要
    ├── 笔记卡片2：...
    └── [空状态提示]
```

**修复方案：**
- 在 `StudioPanel.vue` 末尾新增「已保存笔记」折叠区块
- 通过 `listMyNotes` API 获取当前笔记本相关的保存笔记（按 notebookId 过滤）
- 折叠/展开通过 `ref<boolean>` 控制

---

## 三、问题二详细分析

### 首页未登录精选笔记

**误判：** 路由守卫代码已正确（`meta.requiresAuth: false`，guard 不拦截首页），`loadFeaturedNotebooks()` 已在 `onMounted` 无条件调用。

**真实问题有两个：**

**问题2a（数据问题）：** 数据库中可能暂无 `isFeatured=1` 的笔记本记录。首页代码：
```vue
<section class="section" v-if="featuredList.length > 0">
```
当 `featuredList` 为空时，精选区域不显示，用户以为功能未实现。

**问题2b（交互缺陷）：** `goToNotebook()` 函数对未登录用户直接跳转到 `/user/login`，没有友好的提示弹窗：
```typescript
// 当前代码（不友好）
const goToNotebook = (notebookId) => {
  if (!isLoggedIn.value) {
    router.push('/user/login')  // ← 直接跳转，没有弹窗
    return
  }
  ...
}
```

**修复方案：**
1. 改 `goToNotebook()` 未登录时弹出 `a-modal` 确认框而非直接跳转
2. 为测试添加至少一个精选笔记（通过管理员后台设置），或在前端显示「暂无精选笔记」占位区

---

## 四、问题三详细分析

### 管理员导航入口

**误判：** 不是 GlobalHeader 的问题。`GlobalHeader.vue` 已在上次开发中修正（已登录显示「我的笔记」「个人中心」，管理员额外显示「用户管理」「笔记管理」）。

**真实问题：** `HomePage.vue` 配置了 `meta: { hideLayout: true }` / 实际上 HomePage 自带了独立的顶部导航（`.top-nav`），不使用 `BasicLayout` 的 `GlobalHeader`。

```
路由配置中 / 走 BasicLayout（但 HomePage 自带 top-nav）：
├── BasicLayout 渲染（不含 GlobalHeader，因为 HomePage 无 hideLayout，但是 HomePage 的 layout 是自己的）
实际上首页有 hideLayout meta 吗？→ 查了代码，没有 hideLayout：meta: { requiresAuth: false }
所以 BasicLayout 会渲染 GlobalHeader，但 HomePage 还有自己的 top-nav → 双导航！
```

实际验证路由：
```typescript
{ path: '/', component: HomePage, meta: { requiresAuth: false } }
// 注意：没有 hideLayout: true，所以 BasicLayout 会渲染 GlobalHeader！
```

但 `HomePage.vue` 自带 `.top-nav`，叠加了 `BasicLayout` 中的 `GlobalHeader`，导致**双导航栏**显示。

**修复方案：**
- **方案A（推荐）：** 给首页路由加 `hideLayout: true`，让首页只用自带的 `.top-nav`，保持 NotebookLM 风格
- 在 `.top-nav` 中补充「我的笔记」入口（链接到 `/user/notes`），以及管理员的「用户管理」「笔记管理」入口
- 其他页面（个人中心、我的笔记页）使用 `BasicLayout` 的 `GlobalHeader`，菜单已正确

---

## 五、问题四详细分析

### 问题4a：UserProfilePage 头像预览 Bug

**现象：** 进入个人中心页面，当前头像不显示（空白）

**根本原因：** 头像 URL 从 store 读取的是相对路径（如 `/uploads/avatars/xxx.jpg`），但 Vite 代理配置要求通过 `/api` 前缀访问后端资源：

```typescript
// 当前代码（有问题）
const avatarPreview = ref<string>(loginUser.userAvatar || '')
// template: <a-avatar :src="avatarPreview">
// avatarPreview = "/uploads/avatars/xxx.jpg"
// 实际 URL：http://localhost:5173/uploads/avatars/xxx.jpg  ← 前端静态服务器，404!
```

正确做法应该是：
```typescript
const avatarPreview = ref<string>(loginUser.userAvatar ? '/api' + loginUser.userAvatar : '')
// 实际 URL：http://localhost:5173/api/uploads/avatars/xxx.jpg  ← 通过 Vite 代理到 :8123，正确!
```

**同样的问题在 GlobalHeader 中：**
```html
<!-- GlobalHeader.vue 第28行 -->
<a-avatar :src="loginUserStore.loginUser.userAvatar" />
<!-- 应该是 :src="'/api' + loginUserStore.loginUser.userAvatar" -->
```

**修复方案：** 修改 `UserProfilePage.vue` 初始化 `avatarPreview` 时加 `/api` 前缀；修改 `GlobalHeader.vue` 头像 src。

---

### 问题4b：NotebookDetailPage 封面 URL 重复 /api

**现象：** 笔记本封面上传后可能无法显示，或显示路径错误

**根本原因：**
```typescript
// NotebookDetailPage.vue 中上传成功处理
coverPreview.value = '/api' + res.data.data
// 如果 res.data.data 已经包含 "/uploads/..." 则正确
// 但如果某处已拼接了 /api 前缀，则变成 "/api/api/uploads/..."
```

需要检查后端返回的封面 URL 是 `/uploads/xxx` 还是 `/api/uploads/xxx`。

---

## 六、修复方案总结（开发顺序）

### 步骤一：重建 SavedNoteController.java（问题1a，后端）

```java
@RestController
@RequestMapping("/note")
public class SavedNoteController {
    @PostMapping("/save")   // 保存笔记
    @GetMapping("/list")    // 分页查询当前用户笔记
    @PutMapping("/{id}")    // 修改笔记
    @DeleteMapping("/{id}") // 删除笔记
}
```

### 步骤二：修复头像 URL 前缀（问题4a）

- `UserProfilePage.vue`：`avatarPreview` 初始值加 `/api` 前缀
- `GlobalHeader.vue`：头像 `:src` 加 `/api` 前缀（仅当 `userAvatar` 存在时）

### 步骤三：完善 HomePage top-nav（问题2 + 问题3）

- 给首页路由加 `hideLayout: true`（防止双导航）
- 在 `.top-nav` 中为已登录用户添加「我的笔记」入口
- 管理员用户下拉菜单中已有「用户管理」「笔记管理」（已实现）
- 改 `goToNotebook()` 未登录时弹确认框而非直接跳转

### 步骤四：StudioPanel 新增保存笔记展示区（问题1b）

- 在 `StudioPanel.vue` 末尾新增可折叠「📌 已保存笔记」区域
- 通过 `props.notebookId` 过滤，调用 `listMyNotes` 获取当前笔记本相关笔记
- 用 `SaveNoteDialog` 的「保存成功」事件触发列表刷新

---

## 七、修改文件清单预测

| 文件 | 操作 | 所属问题 |
|------|------|---------|
| `controller/SavedNoteController.java` | 新建 | 1a |
| `lyhm-ai-rag-frontend/src/components/notebook/StudioPanel.vue` | 修改（新增笔记区） | 1b |
| `lyhm-ai-rag-frontend/src/pages/user/UserProfilePage.vue` | 修改（1行，avatar URL 加前缀） | 4a |
| `lyhm-ai-rag-frontend/src/components/GlobalHeader.vue` | 修改（1行，avatar URL 加前缀） | 4a |
| `lyhm-ai-rag-frontend/src/router/index.ts` | 修改（首页加 hideLayout:true）| 3 |
| `lyhm-ai-rag-frontend/src/pages/HomePage.vue` | 修改（goToNotebook 加弹窗，top-nav 加我的笔记入口）| 2/3 |

---

> **状态：等待确认后按以上方案开始修复。**
