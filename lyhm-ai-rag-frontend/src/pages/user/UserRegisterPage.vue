<script lang="ts" setup>
import { reactive, computed } from 'vue'
import { userRegister } from '@/api/userController.ts'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { RuleObject } from 'ant-design-vue/es/form'

const formState = reactive<API.UserRegisterRequest>({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
  role: 'user',
  adminCode: '',
})

const router = useRouter()

const isAdmin = computed(() => formState.role === 'admin')

async function validateCheckPassword(rule: RuleObject, value: string) {
  if (value !== formState.userPassword) {
    return Promise.reject('两次输入的密码不一致')
  }
  return Promise.resolve()
}

async function validateAdminCode(rule: RuleObject, value: string) {
  if (formState.role === 'admin' && !value) {
    return Promise.reject('管理员注册码不能为空')
  }
  return Promise.resolve()
}

const handleSubmit = async () => {
  const res = await userRegister(formState)
  if (res.data.code === 0 && res.data.data) {
    message.success('注册成功')
    router.push({ path: '/user/login', replace: true })
  } else {
    message.error('注册失败，' + res.data.message)
  }
}
</script>

<template>
  <div id="userRegisterPage">
    <h2 class="title">用户注册</h2>

    <a-form :model="formState" name="register" autocomplete="off" @finish="handleSubmit">
      <a-form-item name="userAccount" :rules="[{ required: true, message: '请输入账号' }]">
        <a-input v-model:value="formState.userAccount" placeholder="请输入账号（至少4位）" />
      </a-form-item>

      <a-form-item
        name="userPassword"
        :rules="[
          { required: true, message: '请输入密码' },
          { min: 8, message: '密码长度不能小于 8 位' },
        ]"
      >
        <a-input-password v-model:value="formState.userPassword" placeholder="请输入密码（至少8位）" />
      </a-form-item>

      <a-form-item
        name="checkPassword"
        :rules="[
          { required: true, message: '请输入确认密码' },
          { min: 8, message: '密码长度不能小于 8 位' },
          { validator: validateCheckPassword, trigger: 'blur' },
        ]"
      >
        <a-input-password v-model:value="formState.checkPassword" placeholder="请确认密码" />
      </a-form-item>

      <!-- 角色选择 -->
      <a-form-item name="role" label="注册身份">
        <a-radio-group v-model:value="formState.role">
          <a-radio value="user">普通用户</a-radio>
          <a-radio value="admin">管理员</a-radio>
        </a-radio-group>
      </a-form-item>

      <!-- 管理员注册码（仅 admin 时显示） -->
      <a-form-item
        v-if="isAdmin"
        name="adminCode"
        :rules="[{ validator: validateAdminCode, trigger: 'blur' }]"
      >
        <a-input
          v-model:value="formState.adminCode"
          placeholder="请输入管理员注册码"
          type="password"
        />
      </a-form-item>

      <div class="tips">
        已有账号
        <RouterLink to="/user/login">去登录</RouterLink>
      </div>

      <a-form-item>
        <a-button type="primary" html-type="submit" style="width: 100%">注册</a-button>
      </a-form-item>
    </a-form>
  </div>
</template>

<style>
#userRegisterPage {
  max-width: 480px;
  margin: 80px auto 0;
  padding: 40px;
  background: #fff;
  border-radius: var(--border-radius);
  box-shadow: var(--box-shadow);
}

.title {
  text-align: center;
  margin-bottom: 32px;
  color: var(--primary-color);
  font-weight: 600;
}

.desc {
  text-align: center;
  color: var(--text-sub);
  margin-bottom: 24px;
}

.tips {
  text-align: right;
  color: #bbb;
  font-size: 13px;
  margin-bottom: 16px;
}
</style>
