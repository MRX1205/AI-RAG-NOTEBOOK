<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import {
  listUser,
  addUser,
  adminUpdateUser,
  deleteUser,
  adminResetPassword,
  adminUpdateUserStatus,
} from '@/api/userController'

onMounted(() => {
  fetchData()
})

const data = ref<API.UserVO[]>([])
const total = ref(0)
const loading = ref(false)

const searchParams = reactive({
  pageNum: 1,
  pageSize: 10,
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
  { title: '头像', key: 'avatar', width: 64 },
  { title: '账号', dataIndex: 'userAccount', key: 'userAccount' },
  { title: '用户名', dataIndex: 'userName', key: 'userName' },
  { title: '角色', dataIndex: 'userRole', key: 'userRole', width: 100 },
  { title: '状态', key: 'status', width: 90 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime' },
  { title: '操作', key: 'action', width: 220 },
]

const fetchData = async () => {
  loading.value = true
  try {
    const res = await listUser({
      pageNum: searchParams.pageNum,
      pageSize: searchParams.pageSize,
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

// ===== 新增用户弹窗 =====
const addVisible = ref(false)
const addLoading = ref(false)
const addForm = reactive<API.UserAddRequest>({
  userAccount: '',
  userName: '',
  userRole: 'user',
})

const openAdd = () => {
  addForm.userAccount = ''
  addForm.userName = ''
  addForm.userRole = 'user'
  addVisible.value = true
}

const handleAddOk = async () => {
  if (!addForm.userAccount || addForm.userAccount.length < 4) {
    message.warning('账号长度至少4位')
    return
  }
  addLoading.value = true
  try {
    const res = await addUser(addForm)
    if (res.data.code === 0) {
      message.success('新增成功，默认密码：12345678')
      addVisible.value = false
      fetchData()
    } else {
      message.error('新增失败：' + res.data.message)
    }
  } catch {
    message.error('网络错误')
  } finally {
    addLoading.value = false
  }
}

// ===== 编辑用户弹窗 =====
const editVisible = ref(false)
const editLoading = ref(false)
const editForm = reactive<{ id?: string | number; userName?: string; userRole?: string }>({
  id: undefined,
  userName: '',
  userRole: 'user',
})

const openEdit = (record: API.UserVO) => {
  editForm.id = record.id
  editForm.userName = record.userName || ''
  editForm.userRole = record.userRole || 'user'
  editVisible.value = true
}

const handleEditOk = async () => {
  if (!editForm.id) return
  editLoading.value = true
  try {
    const res = await adminUpdateUser(editForm.id, {
      userName: editForm.userName,
      userRole: editForm.userRole,
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

// ===== 重置密码 =====
const handleResetPassword = (record: API.UserVO) => {
  Modal.confirm({
    title: '重置密码确认',
    content: `确定将用户「${record.userName || record.userAccount}」的密码重置为 12345678？`,
    okText: '确定重置',
    cancelText: '取消',
    onOk: async () => {
      try {
        const res = await adminResetPassword(record.id!)
        if (res.data.code === 0) {
          message.success(res.data.data || '密码已重置为 12345678')
        } else {
          message.error('重置失败：' + res.data.message)
        }
      } catch {
        message.error('网络错误')
      }
    },
  })
}

// ===== 启用/禁用 =====
const handleStatusChange = async (record: API.UserVO, checked: boolean) => {
  const newStatus = checked ? 1 : 0
  const action = checked ? '启用' : '禁用'
  try {
    const res = await adminUpdateUserStatus(record.id!, { status: newStatus })
    if (res.data.code === 0) {
      message.success(`已${action}用户「${record.userName || record.userAccount}」`)
      fetchData()
    } else {
      message.error(`${action}失败：` + res.data.message)
    }
  } catch {
    message.error('网络错误')
  }
}

// ===== 删除用户 =====
const handleDelete = async (id: string | number) => {
  try {
    const res = await deleteUser({ id })
    if (res.data.code === 0) {
      message.success('删除成功')
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

const roleTagColor = (role?: string) => role === 'admin' ? 'gold' : 'blue'
</script>

<template>
  <div class="user-manage-page">
    <div class="page-header">
      <h2>用户管理</h2>
      <a-button type="primary" @click="openAdd">
        <template #icon><PlusOutlined /></template>
        新增用户
      </a-button>
    </div>

    <!-- 搜索 -->
    <div class="search-bar">
      <a-input
        v-model:value="searchParams.userName"
        placeholder="搜索用户名"
        style="width: 200px; margin-right: 12px"
        allow-clear
        @press-enter="doSearch"
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
        <template v-if="column.key === 'avatar'">
          <a-avatar
            :size="36"
            :src="record.userAvatar ? '/api' + record.userAvatar : undefined"
            :style="{ background: 'linear-gradient(135deg,#4a90d9,#7c3aed)' }"
          >
            <template v-if="!record.userAvatar" #icon>
              <span style="font-weight:700; font-size:14px;">
                {{ (record.userName || record.userAccount || '?').charAt(0).toUpperCase() }}
              </span>
            </template>
          </a-avatar>
        </template>

        <template v-else-if="column.key === 'userRole'">
          <a-tag :color="roleTagColor(record.userRole)">
            {{ record.userRole === 'admin' ? '管理员' : '普通用户' }}
          </a-tag>
        </template>

        <template v-else-if="column.key === 'status'">
          <a-switch
            :checked="record.status !== 0"
            checked-children="启用"
            un-checked-children="禁用"
            size="small"
            @change="(checked: boolean) => handleStatusChange(record, checked)"
          />
        </template>

        <template v-else-if="column.key === 'createTime'">
          {{ formatDate(record.createTime) }}
        </template>

        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button size="small" @click="openEdit(record)">编辑</a-button>
            <a-button size="small" @click="handleResetPassword(record)">重置密码</a-button>
            <a-popconfirm
              title="确认删除该用户？"
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

    <!-- 新增用户弹窗 -->
    <a-modal
      v-model:open="addVisible"
      title="新增用户"
      :confirm-loading="addLoading"
      ok-text="创建"
      cancel-text="取消"
      @ok="handleAddOk"
    >
      <a-form layout="vertical" style="margin-top: 16px;">
        <a-form-item label="账号" required>
          <a-input v-model:value="addForm.userAccount" placeholder="账号（至少4位）" />
        </a-form-item>
        <a-form-item label="用户名">
          <a-input v-model:value="addForm.userName" placeholder="昵称（可选）" />
        </a-form-item>
        <a-form-item label="角色">
          <a-select v-model:value="addForm.userRole" style="width: 100%">
            <a-select-option value="user">普通用户</a-select-option>
            <a-select-option value="admin">管理员</a-select-option>
          </a-select>
        </a-form-item>
        <a-alert message="默认密码为 12345678，请提醒用户登录后修改" type="info" show-icon />
      </a-form>
    </a-modal>

    <!-- 编辑用户弹窗 -->
    <a-modal
      v-model:open="editVisible"
      title="编辑用户"
      :confirm-loading="editLoading"
      ok-text="保存"
      cancel-text="取消"
      @ok="handleEditOk"
    >
      <a-form layout="vertical" style="margin-top: 16px;">
        <a-form-item label="用户名">
          <a-input v-model:value="editForm.userName" placeholder="用户名（最多20字）" />
        </a-form-item>
        <a-form-item label="角色">
          <a-select v-model:value="editForm.userRole" style="width: 100%">
            <a-select-option value="user">普通用户</a-select-option>
            <a-select-option value="admin">管理员</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.user-manage-page {
  padding: 24px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
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
