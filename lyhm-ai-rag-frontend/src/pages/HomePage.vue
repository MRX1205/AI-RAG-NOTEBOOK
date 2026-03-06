<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { message } from 'ant-design-vue'
import {
  PlusOutlined,
  DeleteOutlined,
  UserOutlined,
  LogoutOutlined,
  SettingOutlined,
  CrownOutlined,
  LoginOutlined,
} from '@ant-design/icons-vue'
import { listNotebook, deleteNotebook, getFeaturedNotebooks } from '@/api/notebookController'
import { userLogout } from '@/api/userController'
import CreateNotebookDialog from '@/components/notebook/CreateNotebookDialog.vue'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/loginUser'

const router = useRouter()
const loginUserStore = useLoginUserStore()
const loginUser = loginUserStore.loginUser

const notebookList = ref<API.NotebookVO[]>([])
const featuredList = ref<API.FeaturedNotebookVO[]>([])
const loading = ref(false)
const createDialogVisible = ref(false)

const emojiList = ['📓', '📔', '📒', '📕', '📗', '📘', '📙', '🗂️', '📚', '📖', '🦜', '☕', '🦞', '🌿', '🎯', '🧠', '💡', '🔬']

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
  } catch (error) {
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
  } catch (error) {
    message.error('网络错误')
  }
}

// 未登录用户点击精选卡片时的弹窗状态
const loginPromptVisible = ref(false)
const pendingNotebookId = ref<string | number | null>(null)

const goToNotebook = (notebookId: string | number) => {
  if (!isLoggedIn.value) {
    // 未登录：弹出友好提示，而非直接跳转
    pendingNotebookId.value = notebookId
    loginPromptVisible.value = true
    return
  }
  router.push(`/notebook/${notebookId}`)
}

const handleLoginPromptOk = () => {
  loginPromptVisible.value = false
  router.push('/user/login')
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

const userMenuItems = computed(() => {
  const items: any[] = [
    { key: 'myNotes', label: '我的笔记', icon: SettingOutlined },
    { key: 'profile', label: '个人中心', icon: SettingOutlined },
  ]
  if (isAdmin.value) {
    items.push(
      { key: 'userManage', label: '用户管理', icon: UserOutlined },
      { key: 'notebookManage', label: '笔记本管理', icon: CrownOutlined },
    )
  }
  items.push({ key: 'logout', label: '退出登录', icon: LogoutOutlined, danger: true })
  return items
})

const handleMenuClick = ({ key }: { key: string }) => {
  if (key === 'myNotes') router.push('/user/notes')
  else if (key === 'profile') router.push('/user/profile')
  else if (key === 'userManage') router.push('/admin/userManage')
  else if (key === 'notebookManage') router.push('/admin/notebookManage')
  else if (key === 'logout') handleLogout()
}
</script>

<template>
  <div class="home-page">
    <!-- 顶部导航栏 -->
    <header class="top-nav">
      <div class="nav-left">
        <div class="logo">
          <span class="logo-icon">📝</span>
          <span class="logo-text">AI RAG Notebook</span>
        </div>
      </div>
      <div class="nav-right">
        <!-- 已登录状态 -->
        <template v-if="isLoggedIn">
          <a-button type="primary" shape="round" size="large" @click="showCreateDialog" class="create-btn">
            <template #icon><PlusOutlined /></template>
            新建笔记本
          </a-button>
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
              <span class="user-name">{{ userDisplayName }}</span>
            </div>
            <template #overlay>
              <a-menu :items="userMenuItems" @click="handleMenuClick" />
            </template>
          </a-dropdown>
        </template>

        <!-- 未登录状态 -->
        <template v-else>
          <a-button shape="round" size="large" @click="router.push('/user/register')" class="register-btn">
            注册
          </a-button>
          <a-button type="primary" shape="round" size="large" @click="router.push('/user/login')" class="login-btn">
            <template #icon><LoginOutlined /></template>
            登录
          </a-button>
        </template>
      </div>
    </header>

    <!-- 主内容区 -->
    <main class="main-content">

      <!-- 精选笔记本区域（所有人可见）-->
      <section class="section" v-if="featuredList.length > 0">
        <div class="section-header">
          <h2 class="section-title">⭐ 精选笔记本</h2>
          <span class="section-hint" v-if="!isLoggedIn">
            <a href="/user/login" class="login-link">登录</a> 后可创建自己的笔记本
          </span>
        </div>
        <div class="featured-scroll">
          <div
            v-for="nb in featuredList"
            :key="String(nb.id)"
            class="featured-card"
            @click="goToNotebook(nb.id!)"
          >
            <div class="featured-cover">
              <img v-if="nb.coverImage" :src="'/api' + nb.coverImage" class="cover-img" />
              <span v-else class="featured-emoji">{{ getEmoji(nb.id) }}</span>
            </div>
            <div class="featured-info">
              <p class="featured-title">{{ nb.title }}</p>
              <p v-if="nb.summary" class="featured-summary">{{ nb.summary }}</p>
              <p class="featured-user">@{{ nb.userName || '匿名' }}</p>
            </div>
          </div>
        </div>
      </section>

      <!-- 已登录：我的笔记本 -->
      <template v-if="isLoggedIn">
        <a-spin :spinning="loading">
          <section class="section" v-if="notebookList.length > 0">
            <div class="section-header">
              <h2 class="section-title">我的笔记本</h2>
            </div>
            <div class="notebook-grid">
              <!-- 新建卡片 -->
              <div class="notebook-card create-card" @click="showCreateDialog">
                <div class="create-icon-wrapper">
                  <PlusOutlined class="create-icon" />
                </div>
                <span class="create-text">新建笔记本</span>
              </div>

              <!-- 笔记本卡片 -->
              <div
                v-for="notebook in notebookList"
                :key="notebook.id"
                class="notebook-card"
                @click="goToNotebook(notebook.id!)"
              >
                <div class="card-cover">
                  <img
                    v-if="notebook.coverImage"
                    :src="'/api' + notebook.coverImage"
                    class="card-cover-img"
                    alt="封面"
                  />
                  <div v-else class="card-emoji">{{ getEmoji(notebook.id) }}</div>
                </div>
                <div class="card-actions" @click.stop>
                  <a-popconfirm
                    title="确定要删除这个笔记本吗？"
                    ok-text="确定"
                    cancel-text="取消"
                    @confirm="handleDelete(notebook.id!)"
                  >
                    <a-button type="text" size="small" class="action-btn">
                      <template #icon><DeleteOutlined /></template>
                    </a-button>
                  </a-popconfirm>
                </div>
                <h3 class="card-title">{{ notebook.title }}</h3>
                <p class="card-meta">
                  {{ formatDate(notebook.createTime) }}
                  <span class="meta-dot">·</span>
                  {{ notebook.sourceCount || 0 }} 个来源
                </p>
              </div>
            </div>
          </section>

          <!-- 已登录但没有笔记本 -->
          <div v-else-if="!loading" class="empty-state">
            <div class="empty-icon">📚</div>
            <h3 class="empty-title">还没有笔记本</h3>
            <p class="empty-desc">创建一个笔记本，开始与 AI 一起探索知识吧！</p>
            <a-button type="primary" shape="round" size="large" @click="showCreateDialog">
              <template #icon><PlusOutlined /></template>
              创建第一个笔记本
            </a-button>
          </div>
        </a-spin>
      </template>

      <!-- 未登录：欢迎区域 -->
      <template v-else>
        <section class="welcome-section" v-if="featuredList.length === 0">
          <div class="welcome-hero">
            <div class="hero-icon">🧠</div>
            <h1 class="hero-title">AI 驱动的知识笔记本</h1>
            <p class="hero-desc">上传文档，与 AI 对话，生成报告，创建测验 — 让知识唾手可得</p>
            <div class="hero-actions">
              <a-button type="primary" size="large" shape="round" @click="router.push('/user/register')" style="margin-right:12px;">
                免费注册
              </a-button>
              <a-button size="large" shape="round" @click="router.push('/user/login')">
                已有账号？登录
              </a-button>
            </div>
          </div>
        </section>
        <div v-else class="login-prompt">
          <a-button type="primary" size="large" shape="round" @click="router.push('/user/register')" style="margin-right:12px;">
            注册开始使用
          </a-button>
          <a-button size="large" shape="round" @click="router.push('/user/login')">
            登录
          </a-button>
        </div>
      </template>
    </main>

    <!-- 创建笔记本弹窗 -->
    <CreateNotebookDialog
      :visible="createDialogVisible"
      @update:visible="createDialogVisible = $event"
      @success="onCreateSuccess"
    />

    <!-- 未登录点击精选笔记时的提示弹窗 -->
    <a-modal
      v-model:open="loginPromptVisible"
      title="需要登录"
      ok-text="去登录"
      cancel-text="取消"
      @ok="handleLoginPromptOk"
    >
      <div style="text-align: center; padding: 16px 0;">
        <div style="font-size: 48px; margin-bottom: 12px;">🔒</div>
        <p style="font-size: 16px; color: #333; margin: 0 0 8px;">请先登录后查看完整内容</p>
        <p style="font-size: 13px; color: #999; margin: 0;">登录后即可访问笔记本详情、与 AI 对话、保存笔记</p>
      </div>
    </a-modal>
  </div>
</template>

<style scoped>
.home-page {
  min-height: 100vh;
  background: transparent;
}

/* ===== 顶部导航栏 ===== */
.top-nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 32px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--border-color);
  box-shadow: 0 2px 12px rgba(74, 144, 217, 0.05);
  position: sticky;
  top: 0;
  z-index: 100;
}

.nav-left { display: flex; align-items: center; }

.logo { display: flex; align-items: center; gap: 10px; }

.logo-icon { font-size: 28px; }

.logo-text {
  font-size: 20px;
  font-weight: 600;
  color: var(--primary-color);
  letter-spacing: -0.5px;
}

.nav-right { display: flex; align-items: center; gap: 12px; }

.create-btn { font-weight: 500; height: 40px; padding: 0 20px; }

.register-btn { height: 40px; }

.login-btn { height: 40px; }

.user-avatar { background: var(--primary-color); cursor: pointer; }

/* ===== 主内容区 ===== */
.main-content {
  max-width: 1280px;
  margin: 0 auto;
  padding: 32px;
}

.section { margin-bottom: 40px; }

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-title { font-size: 22px; font-weight: 600; color: #1a1a1a; margin: 0; }

.section-hint { font-size: 13px; color: #999; }

.login-link { color: var(--primary-color); text-decoration: none; }

.login-link:hover { text-decoration: underline; }

/* ===== 笔记本卡片网格 ===== */
.notebook-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}

.notebook-card {
  position: relative;
  background: #fff;
  border: 1px solid var(--border-color);
  border-top: 4px solid var(--primary-color);
  border-radius: var(--border-radius);
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  min-height: 140px;
  display: flex;
  flex-direction: column;
  box-shadow: var(--box-shadow);
}

.notebook-card:hover {
  box-shadow: var(--box-shadow-hover);
  border-color: var(--primary-hover);
  transform: translateY(-4px);
}

.create-card {
  border: 2px dashed var(--border-color);
  border-top: 2px dashed var(--border-color);
  justify-content: center;
  align-items: center;
  background: var(--bg-block-light);
  gap: 12px;
  box-shadow: none;
}

.create-card:hover {
  border-color: var(--primary-hover);
  background: var(--bg-block-dark);
  box-shadow: var(--box-shadow-hover);
}

.create-icon-wrapper {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
}

.create-card:hover .create-icon-wrapper { background: var(--primary-color); }

.create-icon { font-size: 24px; color: var(--primary-color); transition: color 0.3s; }

.create-card:hover .create-icon { color: #fff; }

.create-text { font-size: 15px; color: #666; font-weight: 500; }

.card-emoji { font-size: 36px; margin-bottom: 16px; line-height: 1; }

.card-cover { margin-bottom: 12px; }

.card-cover-img { width: 100%; height: 90px; object-fit: cover; border-radius: 8px; display: block; }

.card-actions {
  position: absolute;
  top: 12px;
  right: 12px;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.notebook-card:hover .card-actions { opacity: 1; }

.action-btn { color: #999; }

.action-btn:hover { color: #ff4d4f; }

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.card-meta { font-size: 13px; color: #999; margin: 0; }

.meta-dot { margin: 0 4px; }

/* ===== 空状态 ===== */
.empty-state { text-align: center; padding: 100px 0; }

.empty-icon { font-size: 64px; margin-bottom: 16px; }

.empty-title { font-size: 24px; font-weight: 600; color: #1a1a1a; margin: 0 0 8px; }

.empty-desc { font-size: 16px; color: #999; margin: 0 0 24px; }

/* ===== 导航用户区 ===== */
.nav-user-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
  transition: background 0.2s;
}

.nav-user-btn:hover { background: rgba(74, 144, 217, 0.08); }

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ===== 精选水平滚动 ===== */
.featured-scroll {
  display: flex;
  gap: 16px;
  overflow-x: auto;
  padding-bottom: 12px;
  -ms-overflow-style: none;
  scrollbar-width: none;
}

.featured-scroll::-webkit-scrollbar { display: none; }

.featured-card {
  flex: 0 0 180px;
  width: 180px;
  border-radius: 14px;
  overflow: hidden;
  cursor: pointer;
  box-shadow: 0 2px 12px rgba(74, 144, 217, 0.12);
  background: #fff;
  border: 1px solid var(--border-color);
  transition: transform 0.25s, box-shadow 0.25s;
}

.featured-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(74, 144, 217, 0.2);
}

.featured-cover {
  width: 100%;
  height: 110px;
  background: linear-gradient(135deg, #e8f4fd 0%, #b3d8f5 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.cover-img { width: 100%; height: 100%; object-fit: cover; }

.featured-emoji { font-size: 42px; line-height: 1; }

.featured-info { padding: 10px 12px; }

.featured-title {
  font-size: 13px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.featured-summary {
  font-size: 11px;
  color: #666;
  margin: 0 0 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  line-height: 1.4;
}

.featured-user { font-size: 12px; color: #999; margin: 0; }

/* ===== 未登录欢迎区域 ===== */
.welcome-section {
  display: flex;
  justify-content: center;
  padding: 60px 0;
}

.welcome-hero {
  text-align: center;
  max-width: 600px;
}

.hero-icon { font-size: 80px; margin-bottom: 24px; }

.hero-title { font-size: 40px; font-weight: 700; color: #1a1a1a; margin: 0 0 16px; }

.hero-desc { font-size: 18px; color: #666; line-height: 1.6; margin: 0 0 36px; }

.hero-actions { display: flex; justify-content: center; gap: 12px; }

.login-prompt {
  display: flex;
  justify-content: center;
  padding: 32px 0;
  gap: 12px;
}
</style>
