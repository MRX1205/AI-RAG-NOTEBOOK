<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { message } from 'ant-design-vue'
import {
  PlusOutlined,
  FileTextOutlined,
  FilePdfOutlined,
  FileWordOutlined,
  FileMarkdownOutlined,
  DeleteOutlined,
  SearchOutlined,
} from '@ant-design/icons-vue'
import { listSources, deleteSource } from '@/api/sourceController'
import SourceUploadDialog from '@/components/notebook/SourceUploadDialog.vue'

const props = defineProps<{
  notebookId: string | number
}>()

const emit = defineEmits<{
  (e: 'selectionChange', sourceIds: (string | number)[]): void
  (e: 'sourceCountChange'): void
}>()

const sourceList = ref<API.SourceVO[]>([])
const selectedIds = ref<Set<string | number>>(new Set())
const loading = ref(false)
const uploadDialogVisible = ref(false)
const searchText = ref('')

const isAllSelected = computed(() => {
  return sourceList.value.length > 0 &&
    sourceList.value.every(s => selectedIds.value.has(s.id!))
})

/** 筛选后的来源列表 */
const filteredSources = computed(() => {
  if (!searchText.value.trim()) return sourceList.value
  const keyword = searchText.value.toLowerCase()
  return sourceList.value.filter(s =>
    s.fileName?.toLowerCase().includes(keyword)
  )
})

onMounted(() => {
  loadSources()
})

const loadSources = async () => {
  loading.value = true
  try {
    const res = await listSources(props.notebookId)
    if (res.data.code === 0) {
      sourceList.value = res.data.data || []
      selectedIds.value = new Set(sourceList.value.map(s => s.id!))
      emitSelection()
    }
  } catch (error) {
    message.error('加载来源列表失败')
  } finally {
    loading.value = false
  }
}

const toggleSource = (sourceId: string | number) => {
  if (selectedIds.value.has(sourceId)) {
    selectedIds.value.delete(sourceId)
  } else {
    selectedIds.value.add(sourceId)
  }
  selectedIds.value = new Set(selectedIds.value)
  emitSelection()
}

const toggleAll = () => {
  if (isAllSelected.value) {
    selectedIds.value = new Set()
  } else {
    selectedIds.value = new Set(sourceList.value.map(s => s.id!))
  }
  emitSelection()
}

const emitSelection = () => {
  emit('selectionChange', Array.from(selectedIds.value))
}

const handleDelete = async (sourceId: string | number) => {
  try {
    const res = await deleteSource({ id: sourceId })
    if (res.data.code === 0) {
      message.success('删除成功')
      loadSources()
      emit('sourceCountChange')
    } else {
      message.error('删除失败：' + res.data.message)
    }
  } catch (error) {
    message.error('网络错误')
  }
}

const onUploadSuccess = () => {
  uploadDialogVisible.value = false
  loadSources()
  emit('sourceCountChange')
}

const getFileIcon = (fileType?: string) => {
  switch (fileType) {
    case 'pdf': return FilePdfOutlined
    case 'docx': return FileWordOutlined
    case 'md': return FileMarkdownOutlined
    default: return FileTextOutlined
  }
}

const getFileEmoji = (fileType?: string) => {
  switch (fileType) {
    case 'pdf': return '📄'
    case 'docx': return '📝'
    case 'md': return '📋'
    case 'txt': return '📃'
    default: return '📎'
  }
}

const formatFileSize = (bytes?: number) => {
  if (!bytes) return ''
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}
</script>

<template>
  <div class="source-panel">
    <!-- 面板标题 -->
    <div class="panel-header">
      <h3 class="panel-title">来源</h3>
    </div>

    <!-- 添加来源按钮 -->
    <div class="add-source-area">
      <button class="add-source-btn" @click="uploadDialogVisible = true">
        <PlusOutlined />
        <span>添加来源</span>
      </button>
    </div>

    <!-- 搜索栏 -->
    <div class="search-area" v-if="sourceList.length > 0">
      <div class="search-input-wrapper">
        <SearchOutlined class="search-icon" />
        <input
          v-model="searchText"
          type="text"
          placeholder="在网络中搜索新来源"
          class="search-input"
        />
      </div>
    </div>

    <!-- 全选控制 -->
    <div class="select-all" v-if="sourceList.length > 0">
      <a-checkbox :checked="isAllSelected" @change="toggleAll">
        选择所有来源
      </a-checkbox>
      <span class="select-badge" v-if="isAllSelected">✓</span>
    </div>

    <!-- 来源列表 -->
    <a-spin :spinning="loading">
      <div class="source-list">
        <div
          v-for="source in filteredSources"
          :key="source.id"
          class="source-item"
          :class="{ selected: selectedIds.has(source.id!) }"
          @click="toggleSource(source.id!)"
        >
          <div class="source-checkbox">
            <a-checkbox :checked="selectedIds.has(source.id!)" />
          </div>
          <div class="source-icon-wrapper">
            <span class="source-emoji">{{ getFileEmoji(source.fileType) }}</span>
          </div>
          <div class="source-info">
            <div class="source-name">{{ source.fileName }}</div>
            <div class="source-meta">
              <span v-if="source.status === 'completed'" class="status-completed">
                {{ source.segmentCount }} 个分块
              </span>
              <span v-else-if="source.status === 'processing'" class="status-processing">
                处理中...
              </span>
              <span v-else class="status-failed">
                处理失败
              </span>
            </div>
          </div>
          <div class="source-actions" @click.stop>
            <a-popconfirm
              title="确定删除此来源？"
              ok-text="确定"
              cancel-text="取消"
              @confirm="handleDelete(source.id!)"
            >
              <a-button type="text" danger size="small">
                <template #icon><DeleteOutlined /></template>
              </a-button>
            </a-popconfirm>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="!loading && sourceList.length === 0" class="empty-state">
        <div class="empty-icon">📎</div>
        <div class="empty-title">已保存的来源将显示在此处</div>
        <div class="empty-desc">点击上方的"添加来源"即可添加 PDF、网站、文本、视频或音频文件。</div>
      </div>
    </a-spin>

    <!-- 上传弹窗 -->
    <SourceUploadDialog
      :visible="uploadDialogVisible"
      :notebook-id="notebookId"
      @update:visible="uploadDialogVisible = $event"
      @success="onUploadSuccess"
    />
  </div>
</template>

<style scoped>
.source-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  border-bottom: 1px solid var(--border-color);
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0;
  color: var(--primary-color);
}

/* ========== 添加来源按钮 ========== */
.add-source-area {
  padding: 12px 16px 0;
}

.add-source-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px;
  border: 1px dashed var(--border-color);
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  font-size: 14px;
  color: var(--primary-color);
  transition: all 0.15s;
}

.add-source-btn:hover {
  background: var(--bg-block-light);
  border-color: var(--primary-hover);
}

/* ========== 搜索栏 ========== */
.search-area {
  padding: 12px 16px 0;
}

.search-input-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border: 1px solid var(--border-color);
  border-radius: 20px;
  background: #fff;
  transition: border-color 0.2s;
}

.search-input-wrapper:focus-within {
  border-color: var(--primary-color);
}

.search-icon {
  color: #999;
  font-size: 14px;
  flex-shrink: 0;
}

.search-input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 13px;
  background: transparent;
  color: #3c4043;
}

.search-input::placeholder {
  color: #999;
}

.select-all {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  border-bottom: 1px solid var(--border-color);
  font-size: 13px;
}

.select-badge {
  color: var(--primary-color);
  font-weight: 600;
}

/* ========== 来源列表 ========== */
.source-list {
  flex: 1;
  overflow-y: auto;
  padding: 4px 0;
}

.source-item {
  display: flex;
  align-items: center;
  padding: 8px 16px;
  cursor: pointer;
  transition: background 0.15s;
  gap: 10px;
}

.source-item:hover {
  background: var(--bg-block-light);
}

.source-item.selected {
  background: var(--bg-block-dark);
}

.source-checkbox {
  flex-shrink: 0;
}

.source-icon-wrapper {
  flex-shrink: 0;
}

.source-emoji {
  font-size: 20px;
}

.source-info {
  flex: 1;
  min-width: 0;
}

.source-name {
  font-size: 13px;
  color: #1a1a1a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 500;
}

.source-meta {
  display: flex;
  gap: 8px;
  font-size: 11px;
  margin-top: 2px;
}

.status-completed { color: #52c41a; }
.status-processing { color: var(--primary-color); }
.status-failed { color: #ff4d4f; }

.source-actions {
  opacity: 0;
  transition: opacity 0.2s;
  flex-shrink: 0;
}

.source-item:hover .source-actions {
  opacity: 1;
}

/* ========== 空状态 ========== */
.empty-state {
  padding: 60px 24px;
  text-align: center;
}

.empty-icon {
  font-size: 36px;
  margin-bottom: 16px;
}

.empty-title {
  font-size: 14px;
  font-weight: 500;
  color: #5f6368;
  margin-bottom: 8px;
}

.empty-desc {
  font-size: 13px;
  color: #999;
  line-height: 1.5;
}
</style>
