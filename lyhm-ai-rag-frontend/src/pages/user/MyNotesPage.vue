<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { EditOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import { listMyNotes, updateNote, deleteNote } from '@/api/savedNoteController'

const noteList = ref<API.SavedNoteVO[]>([])
const total = ref(0)
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(12)

onMounted(() => fetchNotes())

const fetchNotes = async () => {
  loading.value = true
  try {
    const res = await listMyNotes({ pageNum: pageNum.value, pageSize: pageSize.value })
    if (res.data.code === 0 && res.data.data) {
      noteList.value = res.data.data.records ?? []
      total.value = res.data.data.totalRow ?? 0
    } else {
      message.error('加载失败：' + res.data.message)
    }
  } catch {
    message.error('网络错误')
  } finally {
    loading.value = false
  }
}

// 编辑弹窗
const editVisible = ref(false)
const editLoading = ref(false)
const editForm = reactive({
  id: undefined as string | number | undefined,
  title: '',
  content: '',
})

const openEdit = (note: API.SavedNoteVO) => {
  editForm.id = note.id
  editForm.title = note.title || ''
  editForm.content = note.content || ''
  editVisible.value = true
}

const handleEditOk = async () => {
  if (!editForm.content.trim()) {
    message.warning('内容不能为空')
    return
  }
  editLoading.value = true
  try {
    const res = await updateNote(editForm.id!, {
      title: editForm.title,
      content: editForm.content,
    })
    if (res.data.code === 0) {
      message.success('修改成功')
      editVisible.value = false
      fetchNotes()
    } else {
      message.error('修改失败：' + res.data.message)
    }
  } catch {
    message.error('网络错误')
  } finally {
    editLoading.value = false
  }
}

const handleDelete = async (id: string | number) => {
  try {
    const res = await deleteNote(id)
    if (res.data.code === 0) {
      message.success('已删除')
      fetchNotes()
    } else {
      message.error('删除失败：' + res.data.message)
    }
  } catch {
    message.error('网络错误')
  }
}

const formatDate = (d?: string) => {
  if (!d) return ''
  return new Date(d).toLocaleDateString('zh-CN', { year: 'numeric', month: 'short', day: 'numeric' })
}
</script>

<template>
  <div class="my-notes-page">
    <div class="page-header">
      <h2 class="page-title">📌 我的笔记</h2>
      <p class="page-desc">保存自 AI 对话的知识卡片</p>
    </div>

    <a-spin :spinning="loading">
      <!-- 空状态 -->
      <div v-if="!loading && noteList.length === 0" class="empty-state">
        <div class="empty-icon">📝</div>
        <h3>还没有保存任何笔记</h3>
        <p>在 AI 对话页面点击「📌 保存到笔记」即可保存 AI 回答</p>
      </div>

      <!-- 卡片列表 -->
      <div v-else class="note-grid">
        <div
          v-for="note in noteList"
          :key="String(note.id)"
          class="note-card"
        >
          <div class="note-card-header">
            <h3 class="note-title">{{ note.title || 'AI笔记' }}</h3>
            <div class="note-actions">
              <a-button type="text" size="small" @click="openEdit(note)">
                <template #icon><EditOutlined /></template>
              </a-button>
              <a-popconfirm
                title="确认删除这条笔记？"
                ok-text="确认"
                cancel-text="取消"
                @confirm="handleDelete(note.id!)"
              >
                <a-button type="text" size="small" danger>
                  <template #icon><DeleteOutlined /></template>
                </a-button>
              </a-popconfirm>
            </div>
          </div>

          <p v-if="note.sourceQuestion" class="note-question">
            Q：{{ note.sourceQuestion }}
          </p>

          <div class="note-content">{{ note.content }}</div>

          <div class="note-footer">
            <span class="note-date">{{ formatDate(note.createTime) }}</span>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div v-if="total > pageSize" class="pagination-wrap">
        <a-pagination
          v-model:current="pageNum"
          :total="total"
          :page-size="pageSize"
          show-quick-jumper
          @change="fetchNotes"
        />
      </div>
    </a-spin>

    <!-- 编辑弹窗 -->
    <a-modal
      v-model:open="editVisible"
      title="编辑笔记"
      :confirm-loading="editLoading"
      ok-text="保存"
      cancel-text="取消"
      width="640px"
      @ok="handleEditOk"
    >
      <a-form layout="vertical" style="margin-top: 8px;">
        <a-form-item label="标题">
          <a-input v-model:value="editForm.title" :maxlength="100" show-count />
        </a-form-item>
        <a-form-item label="内容">
          <a-textarea v-model:value="editForm.content" :rows="10" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.my-notes-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px 24px;
}

.page-header {
  margin-bottom: 32px;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 8px;
  color: #1a1a1a;
}

.page-desc {
  font-size: 15px;
  color: #999;
  margin: 0;
}

.empty-state {
  text-align: center;
  padding: 80px 0;
  color: #999;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.empty-state h3 {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin: 0 0 8px;
}

.empty-state p {
  font-size: 14px;
  margin: 0;
}

.note-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

.note-card {
  background: #fff;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  box-shadow: var(--box-shadow);
  transition: box-shadow 0.25s, transform 0.25s;
}

.note-card:hover {
  box-shadow: var(--box-shadow-hover);
  transform: translateY(-2px);
}

.note-card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
}

.note-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.note-actions {
  display: flex;
  gap: 2px;
  flex-shrink: 0;
}

.note-question {
  font-size: 12px;
  color: var(--primary-color);
  background: var(--bg-block-light);
  border-radius: 6px;
  padding: 6px 10px;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.note-content {
  font-size: 14px;
  color: #444;
  line-height: 1.6;
  flex: 1;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  text-overflow: ellipsis;
}

.note-footer {
  display: flex;
  justify-content: flex-end;
}

.note-date {
  font-size: 12px;
  color: #bbb;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}
</style>
