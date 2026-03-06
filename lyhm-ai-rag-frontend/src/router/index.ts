import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { message } from 'ant-design-vue'
import HomePage from '@/pages/HomePage.vue'
import ExplorePage from '@/pages/ExplorePage.vue'
import ExploreDetailPage from '@/pages/ExploreDetailPage.vue'
import NotebookDetailPage from '@/pages/notebook/NotebookDetailPage.vue'
import UserLoginPage from '@/pages/user/UserLoginPage.vue'
import UserRegisterPage from '@/pages/user/UserRegisterPage.vue'
import UserManagePage from '@/pages/admin/UserManagePage.vue'
import NotebookManagePage from '@/pages/admin/NotebookManagePage.vue'
import AdminDashboardPage from '@/pages/admin/AdminDashboardPage.vue'
import UserProfilePage from '@/pages/user/UserProfilePage.vue'
import AdminLayout from '@/layouts/AdminLayout.vue'
import MyNotesPage from '@/pages/user/MyNotesPage.vue'

// 扩展 RouteMeta 类型
declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean
    requiresAdmin?: boolean
    hideLayout?: boolean
  }
}

const routes: RouteRecordRaw[] = [
  // ===== 公开路由（无需登录）=====
  {
    path: '/',
    component: HomePage,
    // hideLayout: true 让 BasicLayout 不渲染 GlobalHeader，首页使用自带 top-nav
    meta: { requiresAuth: false, hideLayout: true },
  },
  {
    path: '/user/login',
    component: UserLoginPage,
    meta: { requiresAuth: false, hideLayout: true },
  },
  {
    path: '/user/register',
    component: UserRegisterPage,
    meta: { requiresAuth: false, hideLayout: true },
  },
  // ===== 公开精选浏览路由（无需登录）=====
  {
    path: '/explore',
    component: ExplorePage,
    meta: { requiresAuth: false, hideLayout: true },
  },
  {
    path: '/explore/:id',
    component: ExploreDetailPage,
    meta: { requiresAuth: false, hideLayout: true },
  },

  // ===== 需要登录的路由 =====
  {
    path: '/notebook/:id',
    component: NotebookDetailPage,
    meta: { requiresAuth: true, hideLayout: true },
  },
  {
    path: '/user/profile',
    component: UserProfilePage,
    meta: { requiresAuth: true },
  },
  {
    path: '/user/notes',
    component: MyNotesPage,
    meta: { requiresAuth: true },
  },

  // ===== 管理员路由（嵌套 AdminLayout，需管理员权限）=====
  {
    path: '/admin',
    component: AdminLayout,
    redirect: '/admin/dashboard',   // 直接访问 /admin 时跳转到仪表盘
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      {
        path: 'dashboard',
        component: AdminDashboardPage,
        meta: { requiresAuth: true, requiresAdmin: true },
      },
      {
        path: 'userManage',
        component: UserManagePage,
        meta: { requiresAuth: true, requiresAdmin: true },
      },
      {
        path: 'notebookManage',
        component: NotebookManagePage,
        meta: { requiresAuth: true, requiresAdmin: true },
      },
    ],
  },

  // ===== 404 fallback =====
  { path: '/:pathMatch(.*)*', redirect: '/' },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// ===== 全局路由守卫 =====
let initialized = false

router.beforeEach(async (to, _from, next) => {
  // 延迟导入避免循环依赖
  const { useLoginUserStore } = await import('@/stores/loginUser')
  const loginUserStore = useLoginUserStore()

  // 首次访问时尝试获取登录态（避免刷新页面丢失状态）
  if (!initialized) {
    await loginUserStore.fetchLoginUser()
    initialized = true
  }

  const isLoggedIn = !!loginUserStore.loginUser.id
  const isAdmin = loginUserStore.loginUser.userRole === 'admin'

  // 需要登录但未登录
  if (to.meta.requiresAuth && !isLoggedIn) {
    next(`/user/login?redirect=${encodeURIComponent(to.fullPath)}`)
    return
  }

  // 需要管理员权限但当前用户不是管理员
  if (to.meta.requiresAdmin && !isAdmin) {
    message.warning('无权限访问管理页面')
    next('/')
    return
  }

  next()
})

export default router
