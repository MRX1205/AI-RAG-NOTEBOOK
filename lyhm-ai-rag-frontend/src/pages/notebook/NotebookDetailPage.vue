<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ArrowLeftOutlined, PlusOutlined, SettingOutlined, ShareAltOutlined, CameraOutlined, EditOutlined, CheckOutlined, CloseOutlined } from '@ant-design/icons-vue'
import { getNotebookById, uploadNotebookCover, updateNotebook } from '@/api/notebookController'
import SourcePanel from '@/components/notebook/SourcePanel.vue'
import ChatPanel from '@/components/notebook/ChatPanel.vue'
import StudioPanel from '@/components/notebook/StudioPanel.vue'

const route = useRoute()
const router = useRouter()
const notebookId = route.params.id as string

const notebook = ref<API.NotebookVO>({})
const loading = ref(false)
const selectedSourceIds = ref<(string | number)[]>([])

// 封面上传
const coverInputRef = ref<HTMLInputElement | null>(null)
const uploadingCover = ref(false)
const coverPreview = ref<string>('')

// 标题内联编辑
const titleEditing = ref(false)
const titleInput = ref('')
const titleSaving = ref(false)
const titleInputRef = ref<HTMLInputElement | null>(null)

const startTitleEdit = () => {
  titleInput.value = notebook.value.title || ''
  titleEditing.value = true
  // 等 DOM 更新后聚焦
  setTimeout(() => titleInputRef.value?.focus(), 50)
}

const cancelTitleEdit = () => {
  titleEditing.value = false
}

const saveTitleEdit = async () => {
  const newTitle = titleInput.value.trim()
  if (!newTitle) {
    message.warning('标题不能为空')
    return
  }
  if (newTitle === notebook.value.title) {
    titleEditing.value = false
    return
  }
  titleSaving.value = true
  try {
    const res = await updateNotebook({ id: notebookId, title: newTitle })
    if (res.data.code === 0) {
      notebook.value.title = newTitle
      message.success('标题已更新')
      titleEditing.value = false
    } else {
      message.error('更新失败：' + res.data.message)
    }
  } catch {
    message.error('请求失败')
  } finally {
    titleSaving.value = false
  }
}

onMounted(() => {
  loadNotebook()
})

const loadNotebook = async () => {
  loading.value = true
  try {
    const res = await getNotebookById({ id: notebookId })
    if (res.data.code === 0) {
      notebook.value = res.data.data || {}
      coverPreview.value = notebook.value.coverImage || ''
    } else {
      message.error('加载笔记本失败：' + res.data.message)
      router.push('/')
    }
  } catch (error) {
    message.error('网络错误')
    router.push('/')
  } finally {
    loading.value = false
  }
}

const onSourceSelectionChange = (sourceIds: (string | number)[]) => {
  selectedSourceIds.value = sourceIds
}

const onSourceCountChange = () => {
  loadNotebook()
}

const goBack = () => {
  router.push('/')
}

// 封面上传逻辑
const triggerCoverUpload = () => {
  coverInputRef.value?.click()
}

const onCoverFileChange = async (e: Event) => {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return
  const allowed = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
  if (!allowed.includes(file.type)) {
    message.error('只支持 jpg/png/gif/webp 格式')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    message.error('封面不能超过 5MB')
    return
  }
  uploadingCover.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const res = await uploadNotebookCover(notebookId, formData)
    if (res.data.code === 0) {
      // 立即更新本地预览
      coverPreview.value = '/api' + res.data.data
      notebook.value.coverImage = res.data.data
      message.success('封面已更新')
    } else {
      message.error('上传失败：' + res.data.message)
    }
  } catch {
    message.error('上传失败，请稍后重试')
  } finally {
    uploadingCover.value = false
    // 清空 input 以允许重复选同文件
    if (coverInputRef.value) coverInputRef.value.value = ''
  }
}
</script>

<template>
  <div class="notebook-detail-page" v-if="!loading">
    <!-- 顶部导航栏 — 对齐 NotebookLM 风格 -->
    <header class="top-nav">
      <div class="nav-left">
        <button class="back-btn" @click="goBack">
          <ArrowLeftOutlined />
        </button>
        <!-- 封面区域（悬浮显示更换按钮） -->
        <div class="cover-wrapper" @click="triggerCoverUpload">
          <div v-if="coverPreview" class="cover-thumb">
            <img :src="coverPreview" alt="封面" />
          </div>
          <span v-else class="logo-icon">📝</span>
          <div class="cover-overlay">
            <CameraOutlined />
            <span>更换封面</span>
          </div>
          <a-spin v-if="uploadingCover" class="cover-spin" />
        </div>
        <input
          ref="coverInputRef"
          type="file"
          accept="image/jpeg,image/png,image/gif,image/webp"
          style="display:none"
          @change="onCoverFileChange"
        />
        <!-- 标题内联编辑 -->
        <div class="title-wrapper">
          <template v-if="!titleEditing">
            <h1 class="notebook-title" @dblclick="startTitleEdit" :title="'双击修改标题'">
              {{ notebook.title || '加载中...' }}
            </h1>
            <a-button type="text" size="small" class="title-edit-btn" @click="startTitleEdit">
              <template #icon><EditOutlined /></template>
            </a-button>
          </template>
          <template v-else>
            <input
              ref="titleInputRef"
              v-model="titleInput"
              class="title-input"
              maxlength="100"
              @keydown.enter="saveTitleEdit"
              @keydown.esc="cancelTitleEdit"
            />
            <a-button type="text" size="small" :loading="titleSaving" @click="saveTitleEdit" style="color: #52c41a;">
              <template #icon><CheckOutlined /></template>
            </a-button>
            <a-button type="text" size="small" @click="cancelTitleEdit" style="color: #ff4d4f;">
              <template #icon><CloseOutlined /></template>
            </a-button>
          </template>
        </div>
      </div>
      <div class="nav-right">
        <a-button type="primary" shape="round" @click="$router.push('/')">
          <template #icon><PlusOutlined /></template>
          创建笔记本
        </a-button>
        <a-button type="text" shape="circle">
          <template #icon><ShareAltOutlined style="font-size: 16px" /></template>
        </a-button>
        <a-button type="text" shape="circle">
          <template #icon><SettingOutlined style="font-size: 16px" /></template>
        </a-button>
      </div>
    </header>

    <!-- 三栏布局主体 -->
    <div class="three-column-layout">
      <!-- 左栏：来源面板 -->
      <div class="left-panel">
        <SourcePanel
          :notebook-id="notebookId"
          @selection-change="onSourceSelectionChange"
          @source-count-change="onSourceCountChange"
        />
      </div>

      <!-- 中栏：对话面板 -->
      <div class="center-panel">
        <ChatPanel
          :notebook-id="notebookId"
          :selected-source-ids="selectedSourceIds"
        />
      </div>

      <!-- 右栏：Studio 面板 -->
      <div class="right-panel">
        <StudioPanel
          :notebook-id="notebookId"
          :selected-source-ids="selectedSourceIds"
        />
      </div>
    </div>
  </div>

  <!-- 加载状态 -->
  <div v-else class="loading-container">
    <a-spin size="large" tip="加载中..." />
  </div>
</template>

<style scoped>
.notebook-detail-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: transparent;
}

/* ========== 顶部导航栏 ========== */
.top-nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 32px;
  border-bottom: 1px solid var(--border-color);
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(10px);
  box-shadow: 0 2px 12px rgba(74, 144, 217, 0.05);
  flex-shrink: 0;
  height: 60px;
}

.nav-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.back-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: none;
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  color: #5f6368;
  transition: all 0.2s;
}

.back-btn:hover {
  background: var(--bg-block-light);
  color: var(--primary-hover);
}

.logo-icon {
  font-size: 24px;
}

.title-wrapper {
  display: flex;
  align-items: center;
  gap: 4px;
}

.notebook-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
  color: var(--primary-color);
  cursor: pointer;
  max-width: 360px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notebook-title:hover {
  color: var(--primary-hover);
}

.title-edit-btn {
  opacity: 0;
  transition: opacity 0.2s;
  flex-shrink: 0;
}

.title-wrapper:hover .title-edit-btn {
  opacity: 1;
}

.title-input {
  font-size: 18px;
  font-weight: 600;
  color: var(--primary-color);
  border: 1px solid var(--primary-color);
  border-radius: 4px;
  padding: 2px 8px;
  outline: none;
  min-width: 200px;
  max-width: 360px;
  background: #fff;
}

.nav-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* ========== 三栏布局 ========== */
.three-column-layout {
  display: flex;
  flex: 1;
  overflow: hidden;
  padding: 16px;
  gap: 16px;
}

.left-panel {
  width: 300px;
  min-width: 300px;
  border: 1px solid var(--border-color);
  background: #fff;
  border-radius: var(--border-radius);
  box-shadow: var(--box-shadow);
  overflow-y: auto;
}

.center-panel {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background: #fff;
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius);
  box-shadow: var(--box-shadow);
}

.right-panel {
  width: 320px;
  min-width: 320px;
  border: 1px solid var(--border-color);
  background: #fff;
  border-radius: var(--border-radius);
  box-shadow: var(--box-shadow);
  overflow-y: auto;
}

.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
}

/* ========== 封面上传 ========== */
.cover-wrapper {
  position: relative;
  cursor: pointer;
  width: 36px;
  height: 36px;
  border-radius: 6px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.cover-thumb {
  width: 100%;
  height: 100%;
}

.cover-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-wrapper:hover .cover-overlay {
  opacity: 1;
}

.cover-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0,0,0,0.55);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  opacity: 0;
  transition: opacity 0.2s;
  color: #fff;
  font-size: 10px;
}

.cover-overlay .anticon {
  font-size: 14px;
}

.cover-spin {
  position: absolute;
  inset: 0;
  background: rgba(255,255,255,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
