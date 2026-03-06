# 三核心修复思考文档

## 阅读范围

- `pic/` 参考图 × 4（NotebookLM 首页风格、笔记本详情页）
- `ragdocs/` 全部 17 份文档
- 前端 `lyhm-ai-rag-frontend/src/` 全部源码
- 后端 `src/main/java/com/lyhm/airag/` 全部源码
- `src/main/resources/application-local.yml`

---

## 问题一：笔记本封面上传报错"封面保存失败"

### 复现结果

```
java.io.IOException: java.io.FileNotFoundException:
  /private/var/folders/.../T/tomcat.8123.xxx/work/Tomcat/localhost/api/./uploads/covers/UUID.png
  (No such file or directory)
  at StandardMultipartHttpServletRequest$StandardMultipartFile.transferTo(...)
```

### 根本原因（具体到行）

**文件：** `src/main/java/com/lyhm/airag/service/impl/NotebookServiceImpl.java`  
**行号：** 194–197

```java
// 第 194 行 —— 构建的是「相对路径」
Path savePath = Paths.get(uploadDir, "covers", savedFileName);
// 第 195 行 —— Files.createDirectories 成功（基于 JVM 工作目录）
Files.createDirectories(savePath.getParent());
// 第 197 行 —— 关键 BUG：transferTo() 解析相对路径时使用 Tomcat 工作目录
file.transferTo(savePath.toFile());   // ← BUG
```

**原因分析：**  
Spring Boot 的 `StandardMultipartFile.transferTo(File dest)` 内部调用 `this.part.write(dest.getPath())`，即 Servlet API 的 `Part.write(String fileName)`。  
当 `fileName` 是相对路径（如 `./uploads/covers/UUID.png`）时，`Part.write()` 把它解析到 **Tomcat 的工作目录**（`/private/var/folders/.../T/tomcat.xxx/work/Tomcat/localhost/api/`），而不是项目根目录，导致 `FileNotFoundException`。

同样的 BUG 存在于：  
**文件：** `src/main/java/com/lyhm/airag/service/impl/UserServiceImpl.java`  
**行号：** 308  
```java
file.transferTo(savePath.toFile());   // ← 同样 BUG（头像上传）
```

### 修复方案

将 `file.transferTo()` 替换为 `Files.copy(file.getInputStream(), absolutePath, REPLACE_EXISTING)`：

```java
// 修复前
file.transferTo(savePath.toFile());

// 修复后（关键：先转绝对路径，再用 Files.copy）
Path absoluteSavePath = savePath.toAbsolutePath().normalize();
Files.createDirectories(absoluteSavePath.getParent());
try (java.io.InputStream inputStream = file.getInputStream()) {
    java.nio.file.Files.copy(inputStream, absoluteSavePath,
            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
}
```

**修改文件：**
1. `NotebookServiceImpl.java` 第 195–198 行（封面上传）
2. `UserServiceImpl.java` 第 306–309 行（头像上传）

### 验证标准

```bash
curl -s -b $COOKIE -F "file=@test.png" http://localhost:8123/api/notebook/upload/cover/$NB_ID
# 期望：{"code":0,"data":"/uploads/covers/UUID.png","message":"ok"}
# 验证文件存在：ls ./uploads/covers/
```

---

## 问题二：首页布局 —— 对照 pic/ 参考图完整重做

### 参考图分析（pic/截屏2026-03-05 22.11.52.png）

NotebookLM 首页结构：

```
顶部导航栏
├─ 左：Logo（图标 + 文字 "N"）
├─ 中：过滤 Tabs（全部 ✓ | 我的笔记本 | 精选笔记本）
└─ 右：⊞网格/☰列表 | 最近 ↓ | + 新建 按钮 | 用户头像

精选笔记本（标题："精选笔记本"）
└─ 横向 4 张大卡片（高 160-180px），每张：
   ├─ 背景图（全幅，深色遮罩，白色文字叠加）
   ├─ 左上角：平台来源 Logo + 名称
   ├─ 底部：标题（加粗白字）+ 日期 + 来源数量

最近打开过的笔记本（标题："最近打开过的笔记本"）
└─ 网格卡片，每格：
   ├─ 第一个：特殊「+ 新建笔记本」卡片（蓝色 + 圆圈图标）
   └─ 其余：封面 emoji/图片 + 标题 + 日期 · 来源数 + ⋮ 按钮
```

### 当前代码问题

**文件：** `lyhm-ai-rag-frontend/src/pages/HomePage.vue`

| 问题 | 当前状态 | 目标 |
|------|---------|------|
| 精选卡片尺寸 | `flex: 0 0 180px; height: 110px`（太小） | 横向大卡 `min-width: 260px; height: 175px`，背景图全铺 |
| 精选卡片样式 | 白底 + 小图标 + 文字列表 | 深色遮罩 + 白色文字叠加背景图风格 |
| 导航区 | 仅有 Logo + 按钮，无过滤 Tabs | 增加 Tab 组件（全部 / 我的笔记本 / 精选笔记本） |
| 我的笔记本卡片 | 白色卡片，无封面图区域单独区 | 上方封面区 + 下方信息，更接近参考图 |
| 新建卡片 | 在网格第一位（已存在） | 样式调整，突出显示蓝色背景 |

### 修复方案

**不修改功能逻辑**（路由、数据加载、弹窗、权限已在上次修复中完成），只做 UI 重构：

1. **增加 Tab 过滤器**
   - 三个 Tab：「全部」「我的笔记本」「精选笔记本」
   - 已登录时默认显示「全部」；未登录只能看「精选笔记本」Tab 有效

2. **精选卡片重设计**
   - 卡片宽 ≥ 260px，高约 175px
   - 背景图（`coverImage` 存在时）+ 线性渐变遮罩（底部深色，便于阅读白字）
   - 无封面时使用彩色渐变背景（根据 id 取色系）
   - 底部文字叠加：标题 + 日期 + 来源数

3. **我的笔记本卡片微调**
   - 卡片整体保持，给 emoji 展示区更丰富的颜色背景（10 个色系轮换）
   - ⋮ 菜单按钮替代直接显示删除图标

4. **视图切换按钮**（图片右上角有 ⊞ 和 ☰）
   - 增加网格/列表视图切换（仅前端 `ref` 控制，不影响数据）

**修改文件：** `HomePage.vue`（仅修改 `<template>` 和 `<style scoped>`，不改 script 逻辑）

---

## 问题三：管理员专属后台入口

### 当前状态分析

**已实现（上次修复）：**
- `AdminLayout.vue`：侧边栏 + 顶部 Header（用户管理、笔记本管理）
- `UserManagePage.vue`、`NotebookManagePage.vue`：完整功能页面
- Router：`/admin/userManage`、`/admin/notebookManage` 有嵌套路由

**已存在的 Bug：**

**文件：** `lyhm-ai-rag-frontend/src/router/index.ts`  
**行号：** 79–80

```javascript
// ← BUG：自我重定向，一旦触发会无限循环
{ path: '/admin/userManage',    redirect: '/admin/userManage' },
{ path: '/admin/notebookManage', redirect: '/admin/notebookManage' },
```

**缺失功能（用户需求）：**

1. `/admin` → 没有默认重定向，直接访问显示空的 `AdminLayout`（无内容）
2. 没有 `/admin/dashboard` 仪表盘页（统计卡片：用户数、笔记本数、文档数）
3. 没有后台统计接口 `GET /admin/stats`
4. HomePage 的管理员导航：只在下拉菜单里（隐藏）。用户希望有**醒目的"管理后台"按钮**在导航栏

### 修复方案

#### 3a. 路由修复

```javascript
// 删除自我重定向的两条旧路由
// 修改 /admin 父路由加 redirect
{
  path: '/admin',
  component: AdminLayout,
  redirect: '/admin/dashboard',  // ← 新增默认重定向
  meta: { requiresAuth: true, requiresAdmin: true },
  children: [
    { path: 'dashboard', component: AdminDashboardPage, ... },   // ← 新增
    { path: 'userManage', component: UserManagePage, ... },
    { path: 'notebookManage', component: NotebookManagePage, ... },
  ],
}
```

#### 3b. AdminLayout.vue 侧边菜单增加"仪表盘"入口

在菜单项数组首位加：`{ key: 'dashboard', icon: DashboardOutlined, label: '仪表盘' }`  
`handleMenuClick` 增加 `dashboard` 的路由跳转

#### 3c. AdminDashboardPage.vue（新建）

仪表盘展示三个统计卡片：
- 👥 用户总数（`GET /admin/stats` → `userCount`）
- 📚 笔记本总数（`notebookCount`）
- 📄 文档总数（`sourceCount`）

加载方式：`onMounted` 请求接口。

#### 3d. 后端统计接口（新增）

**文件：** `UserController.java`（或新建 `AdminController.java`）

```java
// GET /admin/stats
@GetMapping("/admin/stats")
@AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
public BaseResponse<AdminStatsVO> getAdminStats(HttpServletRequest request) {
    long userCount = userService.count();
    long notebookCount = notebookService.count();
    long sourceCount = sourceService.count();
    return ResultUtils.success(new AdminStatsVO(userCount, notebookCount, sourceCount));
}
```

新建 `AdminStatsVO.java`。

接口放在 `NotebookController.java` 或单独的 `AdminController.java`（推荐后者，职责分离）。

#### 3e. HomePage.vue 增加"管理后台"按钮

在已登录且为管理员时，`nav-right` 区域增加一个醒目的按钮：

```vue
<!-- 管理员专属按钮，放在新建笔记本按钮左边 -->
<a-button
  v-if="isAdmin"
  type="default"
  shape="round"
  size="large"
  @click="router.push('/admin')"
  class="admin-btn"
>
  🔧 管理后台
</a-button>
```

`.admin-btn` 样式：深紫色或深色背景，与普通按钮区分。

---

## 执行顺序

按问题一 → 问题三 → 问题二 的顺序修复：

1. **问题一**（最简单，改两行代码）：  
   修改 `NotebookServiceImpl.java` + `UserServiceImpl.java`，重启验证上传成功

2. **问题三**（后端 + 路由 + 新建页面）：  
   - 新建 `AdminController.java` + `AdminStatsVO.java`
   - 新建 `AdminDashboardPage.vue`
   - 修改 `router/index.ts`（删除自我重定向，加 redirect + dashboard 路由）
   - 修改 `AdminLayout.vue`（加仪表盘菜单项）
   - 修改 `HomePage.vue`（加管理后台按钮）

3. **问题二**（纯前端 UI 重做）：  
   完整重做 `HomePage.vue` 的 template + style，严格对照参考图

---

## 文件变更清单

### 后端
| 文件 | 操作 | 原因 |
|------|------|------|
| `NotebookServiceImpl.java` | 修改 | 修复 `transferTo` 相对路径 BUG |
| `UserServiceImpl.java` | 修改 | 修复 `transferTo` 相对路径 BUG |
| `AdminController.java` | 新建 | 管理员统计接口 `/admin/stats` |
| `AdminStatsVO.java` | 新建 | 统计数据 VO |

### 前端
| 文件 | 操作 | 原因 |
|------|------|------|
| `router/index.ts` | 修改 | 删除自我重定向 + 加 dashboard 路由 |
| `AdminLayout.vue` | 修改 | 增加仪表盘菜单项 |
| `pages/admin/AdminDashboardPage.vue` | 新建 | 仪表盘统计页面 |
| `HomePage.vue` | 修改 | 管理后台按钮 + 全页面 UI 重做 |
| `api/adminController.ts` | 新建 | 封装统计接口调用 |
