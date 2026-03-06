import { defineStore } from 'pinia'
import { reactive } from 'vue'
import { getLoginUser } from '@/api/userController.ts'

export const useLoginUserStore = defineStore('loginUser', () => {
  /**
   * 使用 reactive 而非 ref，确保所有组件通过 loginUserStore.loginUser 访问时
   * 得到的是同一个响应式代理对象，setLoginUser 的更新对所有持有引用的组件立即可见。
   */
  const loginUser = reactive<API.LoginUserVO>({ userName: '未登录' })

  /**
   * 获取当前登录用户信息（刷新页面时调用，从后端拉取最新数据）
   */
  async function fetchLoginUser() {
    try {
      const res = await getLoginUser()
      if (res.data.code === 0 && res.data.data) {
        setLoginUser(res.data.data)
      }
    } catch {
      // 后端不可用时保持默认"未登录"状态
    }
  }

  /**
   * 更新登录用户信息（登录成功、修改用户名/头像后调用）
   * 使用 Object.assign 原地更新，确保所有组件的响应式引用都收到变更通知
   */
  function setLoginUser(newLoginUser: API.LoginUserVO | { userName: string }) {
    // 先清空旧属性（处理退出登录、字段减少的情况）
    Object.keys(loginUser).forEach((k) => delete (loginUser as Record<string, unknown>)[k])
    // 合并新数据
    Object.assign(loginUser, newLoginUser)
  }

  return { loginUser, setLoginUser, fetchLoginUser }
})
