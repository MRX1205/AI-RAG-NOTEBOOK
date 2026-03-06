<template>
  <a-layout-header class="header">
    <a-row :wrap="false">
      <!-- 左侧：Logo和标题 -->
      <a-col flex="200px">
        <RouterLink to="/">
          <div class="header-left">
            <img class="logo" src="@/assets/logo.png" alt="Logo" />
            <h1 class="site-title">掠影航猫应用</h1>
          </div>
        </RouterLink>
      </a-col>
      <!-- 中间：导航菜单 -->
      <a-col flex="auto">
        <a-menu
          v-model:selectedKeys="selectedKeys"
          mode="horizontal"
          :items="menuItems"
          @click="handleMenuClick"
        />
      </a-col>
      <!-- 右侧：用户操作区域 -->
      <a-col>
        <div class="user-login-status">
          <div v-if="loginUserStore.loginUser.id">
            <a-dropdown>
              <a-space>
                <!-- 头像路径为相对路径，需加 /api 前缀通过 Vite 代理访问 -->
              <a-avatar :src="loginUserStore.loginUser.userAvatar ? '/api' + loginUserStore.loginUser.userAvatar : undefined" />
                {{ loginUserStore.loginUser.userName ?? '无名' }}
              </a-space>
              <template #overlay>
                <a-menu>
                  <a-menu-item @click="doLogout">
                    <LogoutOutlined />
                    退出登录
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </div>

          <div v-else>
            <a-button type="primary" href="/user/login">登录</a-button>
          </div>
        </div>

      </a-col>
    </a-row>
  </a-layout-header>
</template>

<script setup lang="ts">
import { computed, h, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { MenuProps } from 'ant-design-vue'
// HTML 展示数据
// JS 中引入 Store
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { userLogout } from '@/api/userController'
import { message } from 'ant-design-vue'
import { LogoutOutlined } from '@ant-design/icons-vue'

const loginUserStore = useLoginUserStore()
const router = useRouter()

// 用户注销
const doLogout = async () => {
  const res = await userLogout()
  if (res.data.code === 0) {
    loginUserStore.setLoginUser({
      userName: '未登录',
    })
    message.success('退出登录成功')
    await router.push('/user/login')
  } else {
    message.error('退出登录失败，' + res.data.message)
  }
}
// 当前选中菜单
const selectedKeys = ref<string[]>(['/'])
// 监听路由变化，更新当前选中菜单
router.afterEach((to) => {
  selectedKeys.value = [to.path]
})

// 菜单配置项
const menuItems = computed(() => {
  const isLoggedIn = !!loginUserStore.loginUser.id
  const isAdmin = loginUserStore.loginUser.userRole === 'admin'

  const items: any[] = [
    { key: '/', label: '首页', title: '首页' },
  ]

  if (isLoggedIn) {
    items.push({ key: '/user/notes', label: '我的笔记', title: '我的笔记' })
    items.push({ key: '/user/profile', label: '个人中心', title: '个人中心' })
  }

  if (isAdmin) {
    items.push({ key: '/admin/userManage', label: '用户管理', title: '用户管理' })
    items.push({ key: '/admin/notebookManage', label: '笔记管理', title: '笔记管理' })
  }

  items.push({ key: 'others', label: '掠影航猫', title: '掠影航猫' })

  return items
})

// 处理菜单点击
const handleMenuClick: MenuProps['onClick'] = (e) => {
  const key = e.key as string
  if (key === 'others') {
    window.open('http://lyhlz.cn', '_blank')
    return
  }
  selectedKeys.value = [key]
  // 跳转到对应页面
  if (key.startsWith('/')) {
    router.push(key)
  }
}
</script>

<style scoped>
.header {
  background: #fff;
  padding: 0 24px;
  border-bottom: 1px solid var(--border-color);
  box-shadow: 0 2px 8px rgba(74, 144, 217, 0.05);
  display: flex;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo {
  height: 40px;
  width: 40px;
}

.site-title {
  margin: 0;
  font-size: 20px;
  color: var(--primary-color);
  font-weight: 600;
  letter-spacing: 0.5px;
}

:deep(.ant-menu-horizontal) {
  border-bottom: none !important;
  line-height: 64px;
}

:deep(.ant-menu-item) {
  transition: all 0.3s ease;
  border-radius: var(--border-radius);
  margin: 0 4px !important;
  padding: 0 16px !important;
  line-height: 40px !important;
  height: 40px !important;
  margin-top: 12px !important;
}

:deep(.ant-menu-item:hover) {
  background-color: var(--bg-block-light) !important;
  color: var(--primary-hover) !important;
}

:deep(.ant-menu-item-selected) {
  background-color: var(--bg-block-light) !important;
  color: var(--primary-color) !important;
  font-weight: 600;
}

:deep(.ant-menu-item::after) {
  display: none !important;
}

.user-login-status {
  display: flex;
  align-items: center;
  height: 64px;
}
</style>
