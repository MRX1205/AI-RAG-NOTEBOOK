# 思考文档：新功能扩展（任务 1-6）

> 更新时间：2026-03-06  
> 分析人：AI Agent  
> 目的：在开始开发前，对 6 个新功能进行系统分析，制定精确开发方案（基于实际代码探查结果）

---

## 一、现有代码实际状态（逐项核查）

### 1.1 后端现有结构

```
src/main/java/com/lyhm/airag/
├── controller/
│   ├── UserController.java        ← 含注册/登录/个人中心/管理员接口
│   ├── NotebookController.java    ← 含精选/封面/管理员接口
│   ├── ChatController.java        ← SSE 流式对话
│   ├── SourceController.java      ← 文件上传
│   ├── ReportController.java      ← 报告生成
│   └── QuizController.java        ← 测验生成
├── model/entity/
│   ├── User.java                  ← 有 status 字段（migration_v3 已加）
│   └── Notebook.java              ← 有 isFeatured/coverImage，无 summary
├── model/dto/
│   └── UserRegisterRequest.java   ← 只有 userAccount/userPassword/checkPassword
└── sql/
    ├── user.sql                   ← 基础表结构
    ├── notebook.sql               ← 笔记本及关联表
    ├── migration_v2.sql           ← notebook 加 isFeatured 字段
    └── migration_v3.sql           ← user 加 status 字段
```

### 1.2 前端现有结构

```
lyhm-ai-rag-frontend/src/
├── pages/
│   ├── HomePage.vue               ← 精选 + 我的笔记本，有登录态分支
│   ├── user/UserRegisterPage.vue  ← 仅有账号/密码/确认密码，无角色选择
│   ├── user/UserProfilePage.vue   ← 头像/用户名/密码已实现
│   ├── admin/UserManagePage.vue   ← 完整：列表/新增/编辑/重置密码/启用禁用
│   └── admin/NotebookManagePage.vue ← 完整：列表/编辑/精选/删除
├── components/
│   ├── GlobalHeader.vue           ← 动态菜单：仅显示"用户管理"（admin才显，缺其他菜单）
│   └── notebook/ChatPanel.vue     ← 有「保存到笔记」按钮，但是 placeholder（弹 message.info）
├── layouts/
│   ├── BasicLayout.vue            ← 通用布局（header + footer）
│   └── AdminLayout.vue            ← 已有，侧边栏含「用户管理」「笔记本管理」
└── router/index.ts                ← 完整路由守卫（requiresAuth + requiresAdmin）
```

### 1.3 逐任务完成度

| 任务 | 后端 | 前端 | 状态 |
|------|------|------|------|
| 任务1：AI对话保存为笔记 | ❌ 完全缺失 | ⚠️ 有占位按钮 | 需全新开发 |
| 任务2：首页精选笔记(未登录) | ✅ 接口已有 | ✅ 已实现 | 需加 summary 字段 |
| 任务3：管理员导航入口 | N/A | ⚠️ 菜单不完整 | 需完善导航菜单 |
| 任务4：管理员用户管理 | ✅ 完整 | ✅ 完整 | 已完成 ✓ |
| 任务5：用户个人信息 | ✅ 完整 | ✅ 完整 | 已完成 ✓ |
| 任务6：注册角色选择 | ❌ 无 role/adminCode 字段 | ❌ 无角色选择UI | 需开发 |

---

## 二、数据库变更汇总

### 2.1 新增表（任务1）

```sql
-- migration_v4.sql
-- 任务1：AI对话保存为笔记
CREATE TABLE `saved_note` (
  `id`              BIGINT          NOT NULL AUTO_INCREMENT     COMMENT '主键',
  `userId`          BIGINT          NOT NULL                   COMMENT '所属用户ID',
  `notebookId`      BIGINT                                     COMMENT '来源笔记本ID',
  `title`           VARCHAR(200)                               COMMENT '笔记标题',
  `content`         TEXT            NOT NULL                   COMMENT 'AI回答正文',
  `sourceQuestion`  VARCHAR(500)                               COMMENT '触发该回答的原始问题',
  `createTime`      DATETIME        NOT NULL DEFAULT NOW()     COMMENT '创建时间',
  `updateTime`      DATETIME        NOT NULL DEFAULT NOW()     COMMENT '更新时间',
  `isDelete`        TINYINT(1)      NOT NULL DEFAULT 0         COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  INDEX `idx_userId` (`userId`),
  INDEX `idx_notebookId` (`notebookId`)
) COMMENT = 'AI对话保存的笔记';
```

### 2.2 修改已有表（任务2）

```sql
-- migration_v4.sql 追加
-- 任务2：notebook 加 summary 字段（isFeatured/coverImage 已有，无需重复添加）
ALTER TABLE `notebook`
    ADD COLUMN `summary` VARCHAR(300) COMMENT '摘要简介'
    AFTER `coverImage`;
```

> **注意：** `cover_url` 不需要加，现有字段为 `coverImage`，功能一致。  
> **注意：** `avatar_url` 不需要加，`user` 表现有字段为 `userAvatar`，功能一致。  
> **注意：** `user.status` 已在 migration_v3.sql 中添加，无需重复。

---

## 三、后端接口设计（需新增/修改的部分）

### 3.1 任务1 — 保存笔记（完全新增）

| HTTP | 路径 | 权限 | 描述 |
|------|------|------|------|
| POST | `/note/save` | 登录 | 保存一条笔记 |
| GET  | `/note/list` | 登录 | 分页获取当前用户笔记列表 |
| PUT  | `/note/{id}` | 登录（仅本人）| 修改笔记标题和内容 |
| DELETE | `/note/{id}` | 登录（仅本人）| 删除笔记 |

**POST /note/save 请求体：**
```json
{
  "notebookId": 123,
  "title": "关于向量检索的理解",
  "content": "向量检索是通过将文本转换为向量...",
  "sourceQuestion": "什么是向量检索？"
}
```

**GET /note/list 参数：** `pageNum=1&pageSize=10`

**PUT /note/{id} 请求体：**
```json
{
  "title": "修改后的标题",
  "content": "修改后的内容"
}
```

### 3.2 任务2 — 精选笔记（需更新 FeaturedNotebookVO 加 summary）

现有接口：`GET /notebook/featured`（已无需鉴权，已实现）

仅需：
- Notebook 实体加 `summary` 字段
- `FeaturedNotebookVO` 加 `summary` 字段
- NotebookManagePage 的编辑弹窗加 summary 输入

### 3.3 任务6 — 注册接口修改

修改 `UserRegisterRequest.java`，增加：
- `role`（String，可选，默认 `"user"`）
- `adminCode`（String，可选）

修改 `UserServiceImpl.userRegister()`，增加管理员注册码校验：
```java
if ("admin".equals(role)) {
    if (!adminRegisterCode.equals(adminCode)) {
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "管理员注册码错误");
    }
}
```

`application.yml` 追加配置：
```yaml
app:
  admin-register-code: "1205"
```

### 3.4 已有接口（确认无需新增）

```
任务3（管理员导航）：无后端工作，纯前端
任务4（用户管理）：全部接口已完整实现
  - GET  /user/list
  - POST /user/add
  - PUT  /user/update/{id}
  - PUT  /user/admin/status/{id}
  - PUT  /user/admin/reset-password/{id}

任务5（用户个人信息）：全部接口已完整实现
  - GET  /user/get/login   ← 获取当前用户信息（含头像URL）
  - PUT  /user/update/username
  - PUT  /user/update/password
  - POST /user/upload/avatar
```

---

## 四、前端变更方案

### 4.1 任务1 — 新增保存笔记功能

**新增文件：**

| 文件 | 描述 |
|------|------|
| `src/api/savedNoteController.ts` | API 封装：saveNote, listNotes, updateNote, deleteNote |
| `src/components/notebook/SaveNoteDialog.vue` | 弹窗：预填标题（问题前20字）和内容，可编辑后提交 |
| `src/pages/user/MyNotesPage.vue` | 我的笔记页面：卡片列表 + 编辑 + 删除 |

**修改文件：**

| 文件 | 修改内容 |
|------|---------|
| `src/components/notebook/ChatPanel.vue` | `saveToNote()` 改为打开 SaveNoteDialog，传入 content 和 question |
| `src/api/typings.d.ts` | 新增 `SavedNote`, `SaveNoteRequest`, `UpdateNoteRequest`, `SavedNoteVO` 类型 |
| `src/router/index.ts` | 新增 `/user/notes` 路由（requiresAuth: true） |
| `src/components/GlobalHeader.vue` | 已登录时菜单加「我的笔记」入口 |

### 4.2 任务2 — 精选笔记完善（改动极小）

**修改文件：**

| 文件 | 修改内容 |
|------|---------|
| `src/api/typings.d.ts` | `FeaturedNotebookVO` 加 `summary?: string` |
| `src/pages/admin/NotebookManagePage.vue` | 编辑弹窗加 summary 文本域（可选） |
| `src/pages/HomePage.vue` | 精选卡片展示 summary 字段（如已有则确认展示逻辑） |

### 4.3 任务3 — 导航菜单完善

**修改文件：`src/components/GlobalHeader.vue`**

当前问题：菜单只有「首页」「用户管理（admin）」「关于」「掠影航猫」，缺少：
- 已登录普通用户：「我的笔记本」「我的笔记」「个人中心」
- 管理员额外：「笔记管理」

目标菜单结构：
```typescript
// 所有已登录用户可见
const baseMenus = [
  { key: '/', label: '首页' },
  { key: '/user/notes', label: '我的笔记' },
  { key: '/user/profile', label: '个人中心' },
]
// 仅管理员额外可见
const adminMenus = [
  { key: '/admin/userManage', label: '用户管理' },
  { key: '/admin/notebookManage', label: '笔记管理' },
]
// 未登录
const publicMenus = [
  { key: '/', label: '首页' },
]
```

### 4.4 任务4 — 管理员用户管理（已完成）

UserManagePage.vue 已有：列表、新增、编辑、重置密码、启用/禁用。**无需改动。**

### 4.5 任务5 — 用户个人信息（已完成）

UserProfilePage.vue 已有：头像上传、用户名修改、密码修改。**无需改动。**

### 4.6 任务6 — 注册页角色选择

**修改文件：`src/pages/user/UserRegisterPage.vue`**

新增内容：
- 角色选择（单选：普通用户 / 管理员）
- 选择「管理员」时显示「注册码」输入框（v-if 控制）
- 表单校验：管理员注册码不能为空

**修改 `src/api/userController.ts`：** `userRegister` 函数的请求体加 `role?` 和 `adminCode?` 字段

**修改 `src/api/typings.d.ts`：** `UserRegisterRequest` 加 `role?` 和 `adminCode?`

---

## 五、任务优先顺序与依赖关系

按需求指定顺序：**任务6 → 任务2 → 任务3 → 任务5 → 任务4 → 任务1**

考虑实际依赖关系分析：

```
任务6（注册角色）          ← 独立，改动最小，先做
  → 任务2（精选完善）      ← 加 summary 字段，改动小
    → 任务3（导航菜单）    ← 纯前端，需要「我的笔记」路由先存在（和任务1有关）
      → 任务5（个人信息）  ← 已完成，仅核查验证
        → 任务4（用户管理）← 已完成，仅核查验证
          → 任务1（保存笔记）← 最大任务，全新开发
```

实际建议调整：任务3 的「我的笔记」菜单依赖任务1 的路由，故把任务3 的菜单完善放到任务1 之后。

---

## 六、详细开发步骤（每项任务）

### 任务6：注册页角色选择

**1. 后端**
- 修改 `UserRegisterRequest.java`：加 `role`、`adminCode` 字段
- 修改 `UserServiceImpl.userRegister()`：管理员注册码校验（从 `@Value` 读配置）
- 修改 `application.yml`：加 `app.admin-register-code: "1205"`

**2. 前端**
- 修改 `typings.d.ts`：`UserRegisterRequest` 加 `role?`、`adminCode?`
- 修改 `UserRegisterPage.vue`：加角色单选 + adminCode 输入框（v-if）

---

### 任务2：精选笔记 summary 字段

**1. SQL**
- 写入 `sql/migration_v4.sql`：`ALTER TABLE notebook ADD COLUMN summary...`

**2. 后端**
- `Notebook.java`：加 `summary` 字段
- `FeaturedNotebookVO.java`：加 `summary` 字段
- `NotebookAdminUpdateRequest.java`：加 `summary` 字段（管理员编辑时可填写）
- `NotebookServiceImpl.getAdminNotebookPage()`：确认映射正确

**3. 前端**
- `typings.d.ts`：`FeaturedNotebookVO` 加 `summary`
- `HomePage.vue`：确认精选卡片展示 summary
- `admin/NotebookManagePage.vue`：编辑弹窗加 summary 文本域

---

### 任务3：导航菜单完善

**修改 `GlobalHeader.vue`：**
- 已登录时：显示「首页」「我的笔记」「个人中心」
- 管理员额外：「用户管理」「笔记管理」
- 未登录：只显示「首页」

---

### 任务5：用户个人信息（核查已完成功能）

**验证已有接口：**
- `GET /user/get/login` → 作为「获取当前用户信息」接口（含 userAvatar）
- 前端 `UserProfilePage.vue` 直接使用 loginUserStore 中的数据

**若 `GET /api/user/profile` 不存在，以 `/user/get/login` 代替。**

---

### 任务4：管理员用户管理（核查已完成功能）

检查 UserManagePage.vue 是否可以正常工作，无需改动。

---

### 任务1：AI对话保存为笔记（全新开发）

**1. SQL**（写入 migration_v4.sql）：创建 `saved_note` 表

**2. 后端**
- 新建 `model/entity/SavedNote.java`
- 新建 `mapper/SavedNoteMapper.java`
- 新建 `model/dto/note/SaveNoteRequest.java`
- 新建 `model/dto/note/UpdateNoteRequest.java`
- 新建 `model/vo/SavedNoteVO.java`
- 新建 `service/SavedNoteService.java`
- 新建 `service/impl/SavedNoteServiceImpl.java`
- 新建 `controller/SavedNoteController.java`

**3. 前端**
- 新建 `api/savedNoteController.ts`（saveNote, listMyNotes, updateNote, deleteNote）
- 修改 `typings.d.ts`：加 SavedNote 相关类型
- 新建 `components/notebook/SaveNoteDialog.vue`
- 修改 `components/notebook/ChatPanel.vue`：`saveToNote()` 改为打开 SaveNoteDialog
- 新建 `pages/user/MyNotesPage.vue`
- 修改 `router/index.ts`：加 `/user/notes` 路由
- 修改 `GlobalHeader.vue`：加「我的笔记」入口（可与任务3合并）

---

## 七、风险点与注意事项

### 7.1 saved_note 表命名规范

项目现有所有表名均使用驼峰命名（如 `quizRecord`），但 SQL 任务中要求 `saved_note`（下划线）。建议统一用 `savedNote` 命名，与项目规范一致。但若直接使用题目中的 `saved_note` 也可以，在 MyBatisPlus 中通过 `@TableName("saved_note")` 映射。

### 7.2 ChatPanel 中的问题传递

`saveToNote()` 触发时需要知道：
- AI 回答内容（`msg.content`）
- 触发该回答的问题（`messages[i-1].content`）
- 当前 notebookId（`props.notebookId`）

需确认消息数组中如何找到对应的上一条用户消息。

### 7.3 `summary` 字段 vs `description` 字段

Notebook 已有 `description` 字段，与任务2 要求的 `summary` 功能高度重叠。考虑两个方案：
- **方案A（推荐）**：直接复用 `description` 字段作为摘要展示（无需加新字段）
- **方案B**：按需求规范新增 `summary` 字段

若选方案A，`FeaturedNotebookVO` 中将 `description` 展示即可，`migration_v4.sql` 中只需创建 `saved_note` 表。

### 7.4 管理员注册码硬编码风险

注册码建议读取 `@Value("${app.admin-register-code:1205}")` 而非硬编码，方便后续修改。

---

## 八、修改文件完整清单

### 后端

| 文件 | 操作 | 任务 |
|------|------|------|
| `sql/migration_v4.sql` | 新增 | 1/2 |
| `model/dto/UserRegisterRequest.java` | 修改（加 role, adminCode） | 6 |
| `service/impl/UserServiceImpl.java` | 修改（register 加 adminCode 校验） | 6 |
| `controller/UserController.java` | 修改（register 方法传 role/adminCode） | 6 |
| `resources/application.yml` | 修改（加 admin-register-code 配置） | 6 |
| `model/entity/Notebook.java` | 修改（加 summary） | 2 |
| `model/vo/FeaturedNotebookVO.java` | 修改（加 summary） | 2 |
| `model/dto/NotebookAdminUpdateRequest.java` | 修改（加 summary） | 2 |
| `service/impl/NotebookServiceImpl.java` | 修改（adminUpdate 处理 summary） | 2 |
| `model/entity/SavedNote.java` | 新增 | 1 |
| `mapper/SavedNoteMapper.java` | 新增 | 1 |
| `model/dto/note/SaveNoteRequest.java` | 新增 | 1 |
| `model/dto/note/UpdateNoteRequest.java` | 新增 | 1 |
| `model/vo/SavedNoteVO.java` | 新增 | 1 |
| `service/SavedNoteService.java` | 新增 | 1 |
| `service/impl/SavedNoteServiceImpl.java` | 新增 | 1 |
| `controller/SavedNoteController.java` | 新增 | 1 |

### 前端

| 文件 | 操作 | 任务 |
|------|------|------|
| `api/typings.d.ts` | 修改（UserRegisterRequest 加 role/adminCode；FeaturedNotebookVO 加 summary；新增 SavedNote 类型） | 1/2/6 |
| `pages/user/UserRegisterPage.vue` | 修改（加角色选择 + adminCode 输入） | 6 |
| `components/GlobalHeader.vue` | 修改（完善菜单项） | 3 |
| `pages/user/MyNotesPage.vue` | 新增 | 1 |
| `components/notebook/SaveNoteDialog.vue` | 新增 | 1 |
| `components/notebook/ChatPanel.vue` | 修改（saveToNote 实现） | 1 |
| `api/savedNoteController.ts` | 新增 | 1 |
| `router/index.ts` | 修改（加 /user/notes 路由） | 1 |
| `pages/admin/NotebookManagePage.vue` | 修改（编辑弹窗加 summary） | 2 |

---

## 九、开发顺序确认

> 请确认以下问题后开始开发：

**Q1：任务2的 `summary` 字段** — 是新增独立字段还是复用 `description`？  
**建议：** 新增独立 `summary` 字段，与 `description`（详细描述）区分，`summary` 用于首页卡片简介展示。

**Q2：任务1中** — `saved_note` 表名使用下划线（`saved_note`）还是驼峰（`savedNote`）？  
**建议：** 使用 `saved_note`（题目规定），通过 MyBatisPlus `@TableName` 注解映射。

**Q3：任务3导航菜单** — 「我的笔记本」是否需要单独页面，还是就是首页（`/`）？  
**建议：** 首页本身就是「我的笔记本」，菜单中「首页」即可，或单独加「我的笔记本」→ `/`。

---

> **状态：等待确认后按 任务6 → 任务2 → 任务3 → 任务5 → 任务4 → 任务1 顺序开发。**
