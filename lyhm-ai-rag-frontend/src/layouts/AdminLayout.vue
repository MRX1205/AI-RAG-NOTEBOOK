<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useLoginUserStore } from '@/stores/loginUser'
import { userLogout } from '@/api/userController'
import { message } from 'ant-design-vue'
import {
  UserOutlined,
  BookOutlined,
  HomeOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  DashboardOutlined,
} from '@ant-design/icons-vue'
import { ref } from 'vue'

const router = useRouter()
const route = useRoute()
const loginUserStore = useLoginUserStore()
// 直接引用 store 中的 reactive 对象，保证头像/用户名修改后实时同步
const loginUser = loginUserStore.loginUser

const collapsed = ref(false)

const selectedKeys = computed(() => {
  if (route.path.includes('dashboard')) return ['dashboard']
  if (route.path.includes('userManage')) return ['userManage']
  if (route.path.includes('notebookManage')) return ['notebookManage']
  return ['dashboard']
})

const menuItems = [
  {
    key: 'dashboard',
    icon: DashboardOutlined,
    label: '仪表盘',
  },
  {
    key: 'userManage',
    icon: UserOutlined,
    label: '用户管理',
  },
  {
    key: 'notebookManage',
    icon: BookOutlined,
    label: '笔记本管理',
  },
]

const handleMenuClick = ({ key }: { key: string }) => {
  router.push(`/admin/${key}`)
}

const goHome = () => {
  router.push('/')
}

const handleLogout = async () => {
  try {
    await userLogout()
    loginUserStore.setLoginUser({ userName: '未登录' })
    router.push('/user/login')
  } catch {
    message.error('登出失败')
  }
}

const userDisplayName = computed(() => loginUser.userName || loginUser.userAccount || '管理员')
const avatarLetter = computed(() => userDisplayName.value.charAt(0).toUpperCase())
</script>

<template>
  <a-layout class="admin-layout">
    <!-- 侧边栏 -->
    <a-layout-sider
      v-model:collapsed="collapsed"
      :trigger="null"
      collapsible
      class="admin-sider"
      width="220"
    >
      <!-- Logo -->
      <div class="sider-logo" @click="goHome">
        <span class="logo-icon">📝</span>
        <span v-if="!collapsed" class="logo-text">管理后台</span>
      </div>

      <!-- 侧边菜单 -->
      <a-menu
        theme="dark"
        mode="inline"
        :selected-keys="selectedKeys"
        :items="menuItems.map(item => ({ key: item.key, label: item.label, icon: () => h(item.icon) }))"
        @click="handleMenuClick"
        class="admin-menu"
      />

      <!-- 底部返回首页 -->
      <div class="sider-footer">
        <a-button type="text" class="home-btn" @click="goHome">
          <template #icon><HomeOutlined /></template>
          <span v-if="!collapsed">返回首页</span>
        </a-button>
      </div>
    </a-layout-sider>

    <a-layout>
      <!-- 顶部 Header -->
      <a-layout-header class="admin-header">
        <div class="header-left">
          <a-button
            type="text"
            class="collapse-btn"
            @click="collapsed = !collapsed"
          >
            <template #icon>
              <MenuFoldOutlined v-if="!collapsed" />
              <MenuUnfoldOutlined v-else />
            </template>
          </a-button>
          <span class="header-title">
            {{
              route.path.includes('dashboard') ? '仪表盘' :
              route.path.includes('userManage') ? '用户管理' : '笔记本管理'
            }}
          </span>
        </div>
        <div class="header-right">
          <a-dropdown :trigger="['click']">
            <div class="header-user">
              <a-avatar
                :size="32"
                :src="loginUser.userAvatar ? '/api' + loginUser.userAvatar : undefined"
                style="background: linear-gradient(135deg, #4a90d9, #7c3aed); cursor:pointer;"
              >
                <template v-if="!loginUser.userAvatar" #icon>
                  <span style="font-weight:700; font-size:13px;">{{ avatarLetter }}</span>
                </template>
              </a-avatar>
              <span class="header-username">{{ userDisplayName }}</span>
            </div>
            <template #overlay>
              <a-menu>
                <a-menu-item @click="goHome"><HomeOutlined /> 返回首页</a-menu-item>
                <a-menu-item @click="handleLogout" style="color:#ff4d4f"><LogoutOutlined /> 退出登录</a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </a-layout-header>

      <!-- 内容区 -->
      <a-layout-content class="admin-content">
        <router-view />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script lang="ts">
import { h } from 'vue'
export default { name: 'AdminLayout' }
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
}

.admin-sider {
  background: #1a2035 !important;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.15);
}

:deep(.ant-layout-sider-children) {
  display: flex;
  flex-direction: column;
}

.sider-logo {
  height: 64px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 20px;
  cursor: pointer;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  flex-shrink: 0;
}

.logo-icon {
  font-size: 24px;
  flex-shrink: 0;
}

.logo-text {
  font-size: 16px;
  font-weight: 700;
  color: #fff;
  white-space: nowrap;
  letter-spacing: 0.5px;
}

.admin-menu {
  flex: 1;
  background: transparent !important;
  border-right: none !important;
  padding-top: 12px;
}

:deep(.ant-menu-dark .ant-menu-item-selected) {
  background: rgba(74, 144, 217, 0.3) !important;
  border-radius: 8px;
}

:deep(.ant-menu-dark .ant-menu-item:hover) {
  background: rgba(255, 255, 255, 0.08) !important;
  border-radius: 8px;
}

.sider-footer {
  padding: 12px 8px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.home-btn {
  width: 100%;
  color: rgba(255, 255, 255, 0.65) !important;
  text-align: left;
}

.home-btn:hover {
  color: #fff !important;
  background: rgba(255, 255, 255, 0.08) !important;
}

.admin-header {
  background: #fff;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  height: 64px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  font-size: 18px;
  color: #555;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
}

.header-right {
  display: flex;
  align-items: center;
}

.header-user {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
  transition: background 0.2s;
}

.header-user:hover {
  background: rgba(74, 144, 217, 0.08);
}

.header-username {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.admin-content {
  background: #f5f7fa;
  min-height: calc(100vh - 64px);
}
</style>
