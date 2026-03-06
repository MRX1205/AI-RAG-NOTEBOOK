<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { message } from 'ant-design-vue'
import {
  PlusOutlined,
  DeleteOutlined,
  LogoutOutlined,
  UserOutlined,
  CrownOutlined,
  LoginOutlined,
  StarOutlined,
} from '@ant-design/icons-vue'
import { listNotebook, deleteNotebook, getFeaturedNotebooks, cloneNotebook } from '@/api/notebookController'
import { userLogout } from '@/api/userController'
import CreateNotebookDialog from '@/components/notebook/CreateNotebookDialog.vue'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/loginUser'

const router = useRouter()
const loginUserStore = useLoginUserStore()
// 直接使用 store 中的 reactive 对象（所有组件共享同一响应式引用）
const loginUser = loginUserStore.loginUser

const notebookList = ref<API.NotebookVO[]>([])
const featuredList = ref<API.FeaturedNotebookVO[]>([])
const loading = ref(false)
const createDialogVisible = ref(false)
// 副本添加中状态（按笔记本id记录，防止重复点击）
const cloningIds = ref<Set<string | number>>(new Set())

// emoji 列表，用于无封面时随机显示
const emojiList = ['📓', '📔', '📒', '📕', '📗', '📘', '📙', '🗂️', '📚', '📖', '🦜', '☕', '🦞', '🌿', '🎯', '🧠', '💡', '🔬']

// 登录状态、管理员状态、显示名称
const isLoggedIn = computed(() => !!loginUser.id)
const isAdmin = computed(() => loginUser.userRole === 'admin')
const userDisplayName = computed(() => loginUser.userName || loginUser.userAccount || '用户')
const avatarLetter = computed(() => userDisplayName.value.charAt(0).toUpperCase())

onMounted(() => {
  // 精选笔记对所有人可见
  loadFeaturedNotebooks()
  // 我的笔记仅登录用户可见
  if (isLoggedIn.value) {
    loadNotebookList()
  }
})

const loadNotebookList = async () => {
  loading.value = true
  try {
    const res = await listNotebook()
    if (res.data.code === 0) {
      notebookList.value = res.data.data || []
    } else {
      message.error('加载笔记本列表失败：' + res.data.message)
    }
  } catch {
    message.error('网络错误，请稍后重试')
  } finally {
    loading.value = false
  }
}

const loadFeaturedNotebooks = async () => {
  try {
    const res = await getFeaturedNotebooks()
    if (res.data.code === 0) {
      featuredList.value = res.data.data || []
    }
  } catch { /* 精选接口失败静默处理 */ }
}

const showCreateDialog = () => {
  if (!isLoggedIn.value) {
    router.push('/user/login')
    return
  }
  createDialogVisible.value = true
}

const onCreateSuccess = () => {
  createDialogVisible.value = false
  loadNotebookList()
}

const handleDelete = async (notebookId: string | number) => {
  try {
    const res = await deleteNotebook({ id: notebookId })
    if (res.data.code === 0) {
      message.success('删除成功')
      loadNotebookList()
    } else {
      message.error('删除失败：' + res.data.message)
    }
  } catch {
    message.error('网络错误')
  }
}

/**
 * 点击精选卡片：所有用户直接进入只读详情页 /explore/:id
 */
const goToExplore = (notebookId: string | number) => {
  router.push(`/explore/${notebookId}`)
}

/**
 * 已登录用户将精选笔记本添加到自己的笔记本（克隆副本）
 */
const handleAddToMine = async (notebookId: string | number, event: Event) => {
  event.stopPropagation()
  if (!isLoggedIn.value) {
    router.push('/user/login')
    return
  }
  if (cloningIds.value.has(notebookId)) return
  cloningIds.value.add(notebookId)
  try {
    const res = await cloneNotebook(notebookId)
    if (res.data.code === 0) {
      message.success('已添加到我的笔记本！')
      // 刷新我的笔记本列表，显示新副本
      await loadNotebookList()
    } else {
      message.error('添加失败：' + res.data.message)
    }
  } catch {
    message.error('添加失败，请稍后重试')
  } finally {
    cloningIds.value.delete(notebookId)
  }
}

const formatDate = (dateStr?: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`
}

const getEmoji = (id?: string | number) => {
  if (!id) return '📓'
  return emojiList[Number(id) % emojiList.length]
}

const handleLogout = async () => {
  try {
    await userLogout()
    loginUserStore.setLoginUser({ userName: '未登录' })
    message.success('已退出登录')
  } catch {
    message.error('登出失败')
  }
}

// 视图模式：网格 / 列表
const viewMode = ref<'grid' | 'list'>('grid')

// 用户下拉菜单（个人中心 + 退出登录）
const userMenuItems = computed(() => {
  type MenuItem = { key: string; label: string; icon: object; danger?: boolean }
  const items: MenuItem[] = [
    { key: 'myNotes', label: '我的笔记', icon: UserOutlined },
    { key: 'profile', label: '个人中心', icon: UserOutlined },
  ]
  items.push({ key: 'logout', label: '退出登录', icon: LogoutOutlined, danger: true })
  return items
})

const handleMenuClick = ({ key }: { key: string }) => {
  if (key === 'myNotes') router.push('/user/notes')
  else if (key === 'profile') router.push('/user/profile')
  else if (key === 'logout') handleLogout()
}

// 10 个彩色背景供笔记本卡片轮换使用
const cardColors = [
  '#fef3e2', '#f3e8f9', '#e8f5e9', '#e3f2fd',
  '#fff3e0', '#fce4ec', '#e8eaf6', '#e0f2f1',
  '#f3e5f5', '#e8f4fd',
]
const getCardColor = (id?: string | number) => {
  if (!id) return cardColors[0]
  return cardColors[Number(id) % cardColors.length]
}

// 精选卡片背景渐变色（无封面时使用）
const featuredGradients = [
  'linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%)',
  'linear-gradient(135deg, #0f2027 0%, #203a43 50%, #2c5364 100%)',
  'linear-gradient(135deg, #200122 0%, #6f0000 100%)',
  'linear-gradient(135deg, #005c97 0%, #363795 100%)',
  'linear-gradient(135deg, #1d4350 0%, #a43931 100%)',
  'linear-gradient(135deg, #2c3e50 0%, #3498db 100%)',
]
const getFeaturedGradient = (id?: string | number) => {
  if (!id) return featuredGradients[0]
  return featuredGradients[Number(id) % featuredGradients.length]
}
</script>

<template>
  <div class="home-page">
    <!-- ===== 顶部导航栏 ===== -->
    <header class="top-nav">
      <!-- 左：Logo -->
      <div class="nav-left">
        <div class="logo" @click="router.push('/')">
          <span class="logo-icon">📝</span>
          <span class="logo-text">AI RAG Notebook</span>
        </div>
      </div>

      <!-- 中：导航链接（所有用户均可见）-->
      <nav class="nav-links">
        <a class="nav-link" @click="router.push('/')">首页</a>
        <a class="nav-link" @click="router.push('/explore')">发现精选</a>
      </nav>

      <!-- 右：操作区 -->
      <div class="nav-right">
        <template v-if="isLoggedIn">
          <!-- 视图切换（已登录时显示）-->
          <div class="view-toggle">
            <button
              class="toggle-btn"
              :class="{ active: viewMode === 'grid' }"
              @click="viewMode = 'grid'"
              title="网格视图"
            >⊞</button>
            <button
              class="toggle-btn"
              :class="{ active: viewMode === 'list' }"
              @click="viewMode = 'list'"
              title="列表视图"
            >☰</button>
          </div>

          <!-- 管理员后台按钮（仅管理员可见）-->
          <a-button
            v-if="isAdmin"
            shape="round"
            @click="router.push('/admin')"
            class="admin-entry-btn"
          >
            🔧 管理后台
          </a-button>

          <!-- 新建笔记本按钮 -->
          <a-button type="primary" shape="round" @click="showCreateDialog" class="create-btn">
            <template #icon><PlusOutlined /></template>
            新建笔记本
          </a-button>

          <!-- 用户头像下拉菜单 -->
          <a-dropdown :trigger="['click']">
            <div class="nav-user-btn">
              <a-avatar
                :size="36"
                :src="loginUser.userAvatar ? '/api' + loginUser.userAvatar : undefined"
                class="user-avatar"
              >
                <template v-if="!loginUser.userAvatar" #icon>
                  <span style="font-weight:700;">{{ avatarLetter }}</span>
                </template>
              </a-avatar>
              <span class="nav-username">{{ userDisplayName }}</span>
            </div>
            <template #overlay>
              <a-menu :items="userMenuItems" @click="handleMenuClick" />
            </template>
          </a-dropdown>
        </template>

        <!-- 未登录 -->
        <template v-else>
          <a-button shape="round" @click="router.push('/user/register')" class="register-btn">注册</a-button>
          <a-button type="primary" shape="round" @click="router.push('/user/login')" class="login-btn">
            <template #icon><LoginOutlined /></template>
            登录
          </a-button>
        </template>
      </div>
    </header>

    <!-- ===== 主内容区 ===== -->
    <main class="main-content">

      <!-- ===== 区域一：精选知识库（所有人可见）===== -->
      <section v-if="featuredList.length > 0" class="section">
        <div class="section-header">
          <h2 class="section-title">✨ 精选知识库</h2>
          <a class="section-more" @click="router.push('/explore')">查看全部 →</a>
        </div>

        <!-- 精选卡片横向滚动，背景图全幅 + 深色遮罩 + 白字 -->
        <div class="featured-scroll">
          <div
            v-for="nb in featuredList"
            :key="String(nb.id)"
            class="featured-card"
            :style="!nb.coverImage ? { background: getFeaturedGradient(nb.id) } : {}"
            @click="goToExplore(nb.id!)"
          >
            <!-- 有封面时显示封面图 -->
            <img
              v-if="nb.coverImage"
              :src="'/api' + nb.coverImage"
              class="featured-bg-img"
              alt=""
            />
            <!-- 深色遮罩：底部渐变使文字清晰可读 -->
            <div class="featured-overlay" />
            <!-- 卡片内容 -->
            <div class="featured-content">
              <div class="featured-author">
                <span class="author-dot">{{ getEmoji(nb.id) }}</span>
                <span class="author-name">{{ nb.userName || '匿名' }}</span>
              </div>
              <div class="featured-bottom">
                <p class="featured-title">{{ nb.title }}</p>
                <p v-if="nb.summary" class="featured-summary">{{ nb.summary }}</p>
                <div class="featured-meta">
                  <span>{{ formatDate(nb.createTime) }}</span>
                  <span v-if="nb.sourceCount" class="meta-sep">·</span>
                  <span v-if="nb.sourceCount">{{ nb.sourceCount }} 个来源</span>
                </div>
                <!-- 已登录用户看到"添加到我的笔记本"按钮 -->
                <div v-if="isLoggedIn" class="featured-add-btn-wrap" @click.stop>
                  <a-button
                    size="small"
                    class="featured-add-btn"
                    :loading="cloningIds.has(nb.id!)"
                    @click="handleAddToMine(nb.id!, $event)"
                  >
                    <template #icon><PlusOutlined /></template>
                    添加到我的笔记本
                  </a-button>
                </div>
                <!-- 未登录用户看到灰色提示按钮 -->
                <div v-else class="featured-add-btn-wrap" @click.stop>
                  <a-button
                    size="small"
                    class="featured-add-btn-disabled"
                    @click="router.push('/user/login')"
                  >
                    登录后可添加到笔记本
                  </a-button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- ===== 未登录 + 无精选：欢迎 Hero ===== -->
      <section
        class="welcome-section"
        v-if="!isLoggedIn && featuredList.length === 0"
      >
        <div class="welcome-hero">
          <div class="hero-icon">🧠</div>
          <h1 class="hero-title">AI 驱动的知识笔记本</h1>
          <p class="hero-desc">上传文档，与 AI 对话，生成报告，创建测验 — 让知识唾手可得</p>
          <div class="hero-actions">
            <a-button type="primary" size="large" shape="round" @click="router.push('/user/register')">免费注册</a-button>
            <a-button size="large" shape="round" @click="router.push('/user/login')">已有账号？登录</a-button>
          </div>
        </div>
      </section>

      <!-- ===== 未登录 + 有精选：简洁注册提示 ===== -->
      <div
        class="login-prompt"
        v-if="!isLoggedIn && featuredList.length > 0"
      >
        <a-button type="primary" size="large" shape="round" @click="router.push('/user/register')">注册开始使用</a-button>
        <a-button size="large" shape="round" @click="router.push('/user/login')">登录</a-button>
      </div>

      <!-- ===== 区域二：我的笔记本（仅登录用户可见）===== -->
      <template v-if="isLoggedIn">
        <a-spin :spinning="loading">
          <section class="section">
            <div class="section-header">
              <h2 class="section-title">📚 我的笔记本</h2>
              <a-button type="primary" shape="round" size="small" @click="showCreateDialog">
                <template #icon><PlusOutlined /></template>
                新建
              </a-button>
            </div>

            <!-- 网格视图 -->
            <div v-if="viewMode === 'grid'" class="notebook-grid">
              <!-- 新建卡片（始终排在第一位）-->
              <div class="notebook-card create-card" @click="showCreateDialog">
                <div class="create-icon-wrapper">
                  <PlusOutlined class="create-icon" />
                </div>
                <span class="create-text">新建笔记本</span>
              </div>

              <!-- 笔记本卡片列表 -->
              <div
                v-for="notebook in notebookList"
                :key="notebook.id"
                class="notebook-card"
                @click="router.push(`/notebook/${notebook.id}`)"
              >
                <!-- 封面/emoji 区域 -->
                <div
                  class="card-cover-area"
                  :style="notebook.coverImage ? {} : { background: getCardColor(notebook.id) }"
                >
                  <img
                    v-if="notebook.coverImage"
                    :src="'/api' + notebook.coverImage"
                    class="card-cover-img"
                    alt=""
                  />
                  <span v-else class="card-emoji">{{ getEmoji(notebook.id) }}</span>
                </div>

                <!-- 更多操作菜单（⋮ 按钮，悬停显示）-->
                <div class="card-actions" @click.stop>
                  <a-dropdown :trigger="['click']">
                    <a-button type="text" size="small" class="more-btn">⋮</a-button>
                    <template #overlay>
                      <a-menu>
                        <a-menu-item @click.stop="router.push(`/notebook/${notebook.id}`)">📖 打开</a-menu-item>
                        <a-menu-divider />
                        <a-menu-item>
                          <a-popconfirm
                            title="确定要删除这个笔记本吗？"
                            ok-text="确定"
                            cancel-text="取消"
                            @confirm.stop="handleDelete(notebook.id!)"
                          >
                            <span style="color:#ff4d4f">🗑️ 删除</span>
                          </a-popconfirm>
                        </a-menu-item>
                      </a-menu>
                    </template>
                  </a-dropdown>
                </div>

                <!-- 标题和元信息 -->
                <div class="card-info">
                  <h3 class="card-title">{{ notebook.title }}</h3>
                  <p class="card-meta">
                    {{ formatDate(notebook.createTime) }}
                    <span class="meta-dot">·</span>
                    {{ notebook.sourceCount || 0 }} 个来源
                  </p>
                </div>
              </div>
            </div>

            <!-- 列表视图 -->
            <div v-else class="notebook-list">
              <div
                v-for="notebook in notebookList"
                :key="notebook.id"
                class="list-item"
                @click="router.push(`/notebook/${notebook.id}`)"
              >
                <div
                  class="list-emoji"
                  :style="notebook.coverImage ? {} : { background: getCardColor(notebook.id) }"
                >
                  <img
                    v-if="notebook.coverImage"
                    :src="'/api' + notebook.coverImage"
                    class="list-cover-img"
                    alt=""
                  />
                  <span v-else>{{ getEmoji(notebook.id) }}</span>
                </div>
                <div class="list-info">
                  <span class="list-title">{{ notebook.title }}</span>
                  <span class="list-meta">{{ formatDate(notebook.createTime) }} · {{ notebook.sourceCount || 0 }} 个来源</span>
                </div>
                <a-popconfirm
                  title="确定要删除？"
                  ok-text="确定"
                  cancel-text="取消"
                  @confirm.stop="handleDelete(notebook.id!)"
                >
                  <a-button type="text" size="small" danger @click.stop>
                    <template #icon><DeleteOutlined /></template>
                  </a-button>
                </a-popconfirm>
              </div>

              <!-- 列表视图的新建条目 -->
              <div class="list-item list-item--create" @click="showCreateDialog">
                <div class="list-emoji" style="background: #e8f4fd;">
                  <PlusOutlined style="font-size:20px; color:#4a90d9;" />
                </div>
                <span class="list-title">新建笔记本</span>
              </div>
            </div>

            <!-- 空状态 -->
            <div v-if="notebookList.length === 0 && !loading" class="empty-state">
              <div class="empty-icon">📚</div>
              <h3 class="empty-title">还没有笔记本</h3>
              <p class="empty-desc">点击新建，开始与 AI 一起探索知识吧！</p>
              <a-button type="primary" shape="round" size="large" @click="showCreateDialog">
                <template #icon><PlusOutlined /></template>
                创建第一个笔记本
              </a-button>
            </div>
          </section>
        </a-spin>
      </template>
    </main>

    <!-- 创建笔记本弹窗 -->
    <CreateNotebookDialog
      :visible="createDialogVisible"
      @update:visible="createDialogVisible = $event"
      @success="onCreateSuccess"
    />
  </div>
</template>

<style scoped>
.home-page {
  min-height: 100vh;
  background: #f5f7fa;
}

/* ========== 顶部导航栏 ========== */
.top-nav {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 0 32px;
  height: 64px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  position: sticky;
  top: 0;
  z-index: 100;
}

/* 左：Logo */
.nav-left { flex-shrink: 0; }
.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}
.logo-icon { font-size: 26px; }
.logo-text { font-size: 18px; font-weight: 700; color: var(--primary-color); letter-spacing: -0.5px; }

/* 中：导航链接（所有用户均可见）*/
.nav-links {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1;
}

.nav-link {
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 500;
  color: #555;
  cursor: pointer;
  text-decoration: none;
  transition: background 0.2s, color 0.2s;
}

.nav-link:hover {
  background: #f0f0f0;
  color: var(--primary-color);
}

/* 右：操作区 */
.nav-right { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }

/* 视图切换按钮 */
.view-toggle {
  display: flex;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  overflow: hidden;
}

.toggle-btn {
  padding: 4px 10px;
  border: none;
  background: #fff;
  font-size: 16px;
  cursor: pointer;
  color: #999;
  transition: background 0.2s, color 0.2s;
  line-height: 1;
}

.toggle-btn.active { background: #e8f4fd; color: var(--primary-color); }

/* 管理员后台按钮 */
.admin-entry-btn {
  background: #1a2035 !important;
  border-color: #1a2035 !important;
  color: #fff !important;
  font-weight: 500;
  font-size: 13px;
}
.admin-entry-btn:hover {
  background: #2a3a5c !important;
  border-color: #2a3a5c !important;
}

/* 新建笔记本按钮 */
.create-btn { font-weight: 500; }

/* 登录/注册按钮 */
.register-btn { height: 36px; }
.login-btn { height: 36px; }

/* 用户头像 + 用户名 */
.nav-user-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 10px;
  border-radius: 20px;
  transition: background 0.2s;
}
.nav-user-btn:hover { background: #f0f0f0; }
.user-avatar { background: var(--primary-color); }
.nav-username {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ========== 主内容区 ========== */
.main-content {
  max-width: 1280px;
  margin: 0 auto;
  padding: 32px 32px 64px;
}

.section { margin-bottom: 40px; }

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.section-title {
  font-size: 20px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0;
}

.section-more {
  font-size: 13px;
  color: var(--primary-color);
  cursor: pointer;
  text-decoration: none;
}
.section-more:hover { text-decoration: underline; }

/* ========== 精选卡片（大尺寸，背景图+遮罩+白字）========== */
.featured-scroll {
  display: flex;
  gap: 16px;
  overflow-x: auto;
  padding-bottom: 8px;
  -ms-overflow-style: none;
  scrollbar-width: none;
}
.featured-scroll::-webkit-scrollbar { display: none; }

.featured-card {
  position: relative;
  flex: 0 0 260px;
  width: 260px;
  height: 195px;
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.25s, box-shadow 0.25s;
  box-shadow: 0 4px 16px rgba(0,0,0,0.15);
}

.featured-card:hover {
  transform: translateY(-5px) scale(1.01);
  box-shadow: 0 12px 32px rgba(0,0,0,0.22);
}

/* 封面图（铺满） */
.featured-bg-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 深色遮罩：底部渐变，让文字清晰 */
.featured-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    to bottom,
    rgba(0,0,0,0.08) 0%,
    rgba(0,0,0,0.25) 40%,
    rgba(0,0,0,0.80) 100%
  );
}

/* 卡片内容（叠在遮罩上）*/
.featured-content {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 12px 14px;
}

/* 顶部：作者信息 */
.featured-author {
  display: flex;
  align-items: center;
  gap: 6px;
}

.author-dot { font-size: 16px; }

.author-name {
  font-size: 11px;
  color: rgba(255,255,255,0.9);
  font-weight: 500;
}

/* 底部：标题 + 摘要 + 元信息 + 添加按钮 */
.featured-bottom { display: flex; flex-direction: column; gap: 3px; }

.featured-title {
  font-size: 14px;
  font-weight: 700;
  color: #fff;
  margin: 0;
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
}

.featured-summary {
  font-size: 11px;
  color: rgba(255,255,255,0.75);
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.featured-meta {
  font-size: 11px;
  color: rgba(255,255,255,0.65);
  display: flex;
  align-items: center;
  gap: 4px;
}

.meta-sep { margin: 0 2px; }

/* 添加到我的笔记本按钮 */
.featured-add-btn-wrap { margin-top: 4px; }

.featured-add-btn {
  background: rgba(255,255,255,0.2) !important;
  border-color: rgba(255,255,255,0.5) !important;
  color: #fff !important;
  font-size: 11px !important;
  height: 24px !important;
  padding: 0 8px !important;
  border-radius: 12px !important;
}
.featured-add-btn:hover {
  background: rgba(255,255,255,0.35) !important;
}

.featured-add-btn-disabled {
  background: rgba(255,255,255,0.1) !important;
  border-color: rgba(255,255,255,0.25) !important;
  color: rgba(255,255,255,0.55) !important;
  font-size: 11px !important;
  height: 24px !important;
  padding: 0 8px !important;
  border-radius: 12px !important;
  cursor: pointer !important;
}

/* ========== 笔记本卡片网格 ========== */
.notebook-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}

.notebook-card {
  position: relative;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
  display: flex;
  flex-direction: column;
}

.notebook-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(0,0,0,0.1);
}

/* 封面/emoji 区域 */
.card-cover-area {
  width: 100%;
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
}

.card-cover-img { width: 100%; height: 100%; object-fit: cover; }

.card-emoji { font-size: 48px; line-height: 1; }

/* 标题和元信息 */
.card-info {
  padding: 12px 14px 14px;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.card-title {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-meta { font-size: 12px; color: #999; margin: 0; }

.meta-dot { margin: 0 3px; }

/* ⋮ 操作菜单按钮（悬停显示）*/
.card-actions {
  position: absolute;
  top: 8px;
  right: 8px;
  opacity: 0;
  transition: opacity 0.2s;
}

.notebook-card:hover .card-actions { opacity: 1; }

.more-btn {
  font-size: 18px;
  color: rgba(255,255,255,0.9);
  background: rgba(0,0,0,0.35) !important;
  border-radius: 6px !important;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

/* 新建卡片（虚线边框样式）*/
.create-card {
  background: #f8fbff;
  border: 2px dashed #b3d4f5;
  box-shadow: none;
  min-height: 170px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.create-card:hover {
  border-color: var(--primary-color);
  background: #e8f4fd;
}

.create-icon-wrapper {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(74,144,217,0.2);
  transition: background 0.2s;
}

.create-card:hover .create-icon-wrapper { background: var(--primary-color); }

.create-icon { font-size: 22px; color: var(--primary-color); transition: color 0.2s; }

.create-card:hover .create-icon { color: #fff; }

.create-text { font-size: 14px; font-weight: 500; color: #666; }

/* ========== 列表视图 ========== */
.notebook-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.list-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 16px;
  background: #fff;
  border-radius: 10px;
  border: 1px solid #e8e8e8;
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.2s;
}

.list-item:hover {
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
  transform: translateX(4px);
}

.list-item--create { border: 2px dashed #b3d4f5; background: #f8fbff; }
.list-item--create:hover { border-color: var(--primary-color); }

.list-emoji {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
  overflow: hidden;
}

.list-cover-img { width: 100%; height: 100%; object-fit: cover; }

.list-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  overflow: hidden;
}

.list-title {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.list-meta { font-size: 12px; color: #999; }

/* ========== 空状态 ========== */
.empty-state { text-align: center; padding: 80px 0; }
.empty-icon { font-size: 64px; margin-bottom: 16px; }
.empty-title { font-size: 22px; font-weight: 600; color: #1a1a1a; margin: 0 0 8px; }
.empty-desc { font-size: 15px; color: #999; margin: 0 0 24px; }

/* ========== 欢迎 Hero ========== */
.welcome-section {
  display: flex;
  justify-content: center;
  padding: 80px 0;
}

.welcome-hero { text-align: center; max-width: 600px; }

.hero-icon { font-size: 80px; margin-bottom: 20px; }

.hero-title { font-size: 38px; font-weight: 700; color: #1a1a1a; margin: 0 0 14px; }

.hero-desc { font-size: 17px; color: #666; line-height: 1.6; margin: 0 0 32px; }

.hero-actions { display: flex; justify-content: center; gap: 12px; }

/* ========== 未登录有精选时的注册提示 ========== */
.login-prompt {
  display: flex;
  justify-content: center;
  padding: 28px 0;
  gap: 12px;
}
</style>
