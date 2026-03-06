<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/loginUser'
import { adminListNotebooks, adminUpdateNotebook, adminToggleFeatured, adminDeleteNotebook } from '@/api/notebookController'

const router = useRouter()
const loginUserStore = useLoginUserStore()

// 路由鉴权
onMounted(() => {
  if (loginUserStore.loginUser.userRole !== 'admin') {
    message.warning('无权限访问')
    router.push('/')
    return
  }
  fetchData()
})

const BASE_URL = '/api'

const data = ref<API.NotebookVO[]>([])
const total = ref(0)
const loading = ref(false)

const searchParams = reactive({
  pageNum: 1,
  pageSize: 10,
  title: '',
  userName: '',
})

const pagination = computed(() => ({
  current: searchParams.pageNum,
  pageSize: searchParams.pageSize,
  total: total.value,
  showSizeChanger: true,
  showTotal: (t: number) => `共 ${t} 条`,
}))

const columns = [
  { title: '封面', dataIndex: 'coverImage', key: 'cover', width: 80 },
  { title: '标题', dataIndex: 'title', key: 'title' },
  { title: '所属用户', dataIndex: 'userName', key: 'userName' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime' },
  { title: '精选', dataIndex: 'isFeatured', key: 'isFeatured', width: 80 },
  { title: '操作', key: 'action', width: 160 },
]

const fetchData = async () => {
  loading.value = true
  try {
    const res = await adminListNotebooks({
      pageNum: searchParams.pageNum,
      pageSize: searchParams.pageSize,
      title: searchParams.title || undefined,
      userName: searchParams.userName || undefined,
    })
    if (res.data.code === 0 && res.data.data) {
      data.value = res.data.data.records ?? []
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

const doSearch = () => {
  searchParams.pageNum = 1
  fetchData()
}

const doTableChange = (page: any) => {
  searchParams.pageNum = page.current
  searchParams.pageSize = page.pageSize
  fetchData()
}

const handleToggleFeatured = async (record: API.NotebookVO, val: boolean) => {
  try {
    const res = await adminToggleFeatured(record.id!, val ? 1 : 0)
    if (res.data.code === 0) {
      message.success(val ? '已设为精选' : '已取消精选')
      fetchData()
    } else {
      message.error('操作失败：' + res.data.message)
    }
  } catch {
    message.error('网络错误')
  }
}

// 编辑弹窗
const editVisible = ref(false)
const editForm = reactive<API.NotebookAdminUpdateRequest & { id?: string | number }>({
  id: undefined,
  title: '',
  description: '',
  summary: '',
  isFeatured: 0,
})
const editLoading = ref(false)

const openEdit = (record: API.NotebookVO) => {
  editForm.id = record.id
  editForm.title = record.title || ''
  editForm.description = record.description || ''
  editForm.summary = (record as any).summary || ''
  editForm.isFeatured = record.isFeatured ?? 0
  editVisible.value = true
}

const handleEditOk = async () => {
  if (!editForm.title?.trim()) {
    message.warning('标题不能为空')
    return
  }
  editLoading.value = true
  try {
    const res = await adminUpdateNotebook(editForm.id!, {
      title: editForm.title,
      description: editForm.description,
      summary: editForm.summary,
      isFeatured: editForm.isFeatured,
    })
    if (res.data.code === 0) {
      message.success('修改成功')
      editVisible.value = false
      fetchData()
    } else {
      message.error('修改失败：' + res.data.message)
    }
  } catch {
    message.error('网络错误')
  } finally {
    editLoading.value = false
  }
}

// ===== 软删除 =====
const handleDelete = async (id: string | number) => {
  try {
    const res = await adminDeleteNotebook(id)
    if (res.data.code === 0) {
      message.success('已删除')
      fetchData()
    } else {
      message.error('删除失败：' + res.data.message)
    }
  } catch {
    message.error('网络错误')
  }
}

const formatDate = (dateStr?: string) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleDateString('zh-CN')
}
</script>

<template>
  <div class="notebook-manage-page">
    <div class="page-header">
      <h2>笔记本管理</h2>
    </div>

    <!-- 搜索 -->
    <div class="search-bar">
      <a-input
        v-model:value="searchParams.title"
        placeholder="搜索标题"
        style="width: 180px; margin-right: 12px"
        allow-clear
      />
      <a-input
        v-model:value="searchParams.userName"
        placeholder="搜索用户名"
        style="width: 180px; margin-right: 12px"
        allow-clear
      />
      <a-button type="primary" @click="doSearch">搜索</a-button>
    </div>

    <!-- 表格 -->
    <a-table
      :columns="columns"
      :data-source="data"
      :pagination="pagination"
      :loading="loading"
      row-key="id"
      @change="doTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'cover'">
          <img
            v-if="record.coverImage"
            :src="`/api${record.coverImage}`"
            style="width: 56px; height: 40px; object-fit: cover; border-radius: 4px;"
          />
          <div
            v-else
            style="width: 56px; height: 40px; background: linear-gradient(135deg,#e8f4fd,#b3d8f5); border-radius: 4px; display:flex; align-items:center; justify-content:center; font-size:18px; color:#4a90d9; font-weight:700;"
          >{{ record.title?.charAt(0) || '📓' }}</div>
        </template>
        <template v-else-if="column.key === 'createTime'">
          {{ formatDate(record.createTime) }}
        </template>
        <template v-else-if="column.key === 'isFeatured'">
          <a-switch
            :checked="record.isFeatured === 1"
            checked-children="精选"
            un-checked-children="否"
            @change="(val: boolean) => handleToggleFeatured(record, val)"
          />
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button size="small" @click="openEdit(record)">编辑</a-button>
            <a-popconfirm
              title="确认删除该笔记本？（软删除，不影响关联数据）"
              ok-text="确认"
              cancel-text="取消"
              @confirm="handleDelete(record.id!)"
            >
              <a-button size="small" danger>删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 编辑弹窗 -->
    <a-modal
      v-model:open="editVisible"
      title="编辑笔记本"
      :confirm-loading="editLoading"
      @ok="handleEditOk"
    >
      <a-form layout="vertical">
        <a-form-item label="标题">
          <a-input v-model:value="editForm.title" placeholder="笔记本标题" />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="editForm.description" :rows="2" placeholder="笔记本描述（可选）" />
        </a-form-item>
        <a-form-item label="摘要简介" extra="精选首页展示的简短摘要（最多50字）">
          <a-textarea v-model:value="editForm.summary" :rows="2" :maxlength="50" show-count placeholder="精选卡片摘要（可选）" />
        </a-form-item>
        <a-form-item label="精选">
          <a-switch
            :checked="editForm.isFeatured === 1"
            checked-children="是"
            un-checked-children="否"
            @change="(val: boolean) => { editForm.isFeatured = val ? 1 : 0 }"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.notebook-manage-page {
  padding: 24px;
}
.page-header {
  margin-bottom: 16px;
}
.page-header h2 {
  font-size: 22px;
  font-weight: 700;
  margin: 0;
}
.search-bar {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}
</style>
