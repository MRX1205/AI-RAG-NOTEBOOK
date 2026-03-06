# 阶段报告：系统联调 + UI 优化

> 日期：2026-03-06  
> 阶段：阶段五（优化完善）

## 本阶段完成内容

### 1. 前后端连通性修复

**问题**：`request.ts` 中 Axios baseURL 硬编码为 `http://localhost:8123/api`，前端开发服务器（端口 5173/5174）直接请求后端（端口 8123）依赖 CORS，不利于部署。

**修复方案**：
- `request.ts` 的 `baseURL` 改为相对路径 `/api`
- `vite.config.ts` 添加 `server.proxy` 配置，将 `/api` 代理到 `http://localhost:8123`
- 同样修复了 `chatController.ts` 和 `StudioPanel.vue` 中的 SSE 硬编码 URL

### 2. UI 全面重设计（对齐 NotebookLM 原型图）

参考用户提供的 4 张 Google NotebookLM 截图，重设计了以下 5 个核心组件：

| 组件 | 改动内容 |
|------|---------|
| `HomePage.vue` | 自定义顶部导航栏（Logo + 创建按钮 + 设置 + 用户头像）；笔记本卡片带 emoji 图标、日期、来源数量 |
| `NotebookDetailPage.vue` | NotebookLM 风格导航栏（返回按钮 + Logo + 标题 + 创建/分享/设置）；三栏宽度调整为 300/flex/320 |
| `StudioPanel.vue` | 2×5 彩色工具卡片网格（报告/测验可用，其余 7 个显示"即将推出"）；底部添加笔记按钮 |
| `ChatPanel.vue` | AI 回复底部操作按钮（📌保存到笔记 / 复制 / 点赞 / 点踩）；自定义输入栏带来源计数标签 |
| `SourcePanel.vue` | 独立"+ 添加来源"按钮；搜索栏；"选择所有来源"复选框；emoji 文件图标 |

### 3. 布局系统修复

**问题**：`BasicLayout.vue` 全局包裹了 `GlobalHeader` + `GlobalFooter`，导致自定义导航栏页面出现双头部。

**修复**：
- `router/index.ts` — 为首页和详情页路由添加 `meta: { hideLayout: true }`
- `BasicLayout.vue` — 根据路由 `meta.hideLayout` 条件渲染全局 Header/Footer

## 修改/新增的文件列表

| 文件路径 | 操作 | 说明 |
|---------|------|------|
| `lyhm-ai-rag-frontend/src/request.ts` | 修改 | baseURL: `/api` |
| `lyhm-ai-rag-frontend/vite.config.ts` | 修改 | 添加 proxy 配置 |
| `lyhm-ai-rag-frontend/src/pages/HomePage.vue` | 重写 | NotebookLM 风格首页 |
| `lyhm-ai-rag-frontend/src/pages/notebook/NotebookDetailPage.vue` | 重写 | 自定义导航 + 三栏布局 |
| `lyhm-ai-rag-frontend/src/components/notebook/StudioPanel.vue` | 重写 | 彩色工具卡片网格 |
| `lyhm-ai-rag-frontend/src/components/notebook/ChatPanel.vue` | 重写 | 消息操作按钮 + 自定义输入 |
| `lyhm-ai-rag-frontend/src/components/notebook/SourcePanel.vue` | 重写 | 搜索 + 添加 + 全选 |
| `lyhm-ai-rag-frontend/src/layouts/BasicLayout.vue` | 修改 | 条件隐藏全局 Header/Footer |
| `lyhm-ai-rag-frontend/src/router/index.ts` | 修改 | 添加 hideLayout 元数据 |

## 思考过程

1. **连通性分析**：发现 `request.ts` 硬编码 URL 是最大连通性风险。Vite 代理是标准做法，CORS 配置已存在但代理更可靠。

2. **UI 设计决策**：
   - NotebookLM 的 Studio 面板有 9 种工具类型，但本项目只实现了报告和测验。选择展示全部 9 个工具卡片，其中 7 个标记为"即将推出"，保持与原型图一致的视觉效果。
   - 首页去掉了原型图中的"精选笔记本"区域（当前数据量不需要分区），只保留"我的笔记本"网格。

3. **布局冲突**：在浏览器验证时发现 `BasicLayout` 的全局 Header/Footer 与自定义导航产生冲突。通过路由 meta 机制优雅解决，不影响登录/注册等页面。

## 编译验证

```
✅ 前端 vue-tsc --noEmit → 无错误
✅ 后端 mvn clean compile → BUILD SUCCESS (72 source files, 2.3s)
✅ 前端 dev server 正常启动
```

## 下一步计划

1. **全流程功能测试**：启动后端，登录后测试所有页面和功能
2. **来源上传流程**：验证文件上传 → RAG 处理 → 向量化完整链路
3. **AI 对话测试**：验证 SSE 流式对话 + 引用标注
4. **报告/测验生成**：验证 AI 生成功能完整性
