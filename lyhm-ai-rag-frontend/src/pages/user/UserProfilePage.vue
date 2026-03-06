<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { UserOutlined, UploadOutlined, LockOutlined, EditOutlined } from '@ant-design/icons-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { updateUsername, updatePassword, uploadAvatar } from '@/api/userController'
import { useRouter } from 'vue-router'

const router = useRouter()
const loginUserStore = useLoginUserStore()
const loginUser = loginUserStore.loginUser

// ===== 头像 =====
// 头像路径为相对路径（如 /uploads/avatars/xxx.jpg），需加 /api 前缀才能通过 Vite 代理访问后端
const avatarPreview = ref<string>(loginUser.userAvatar ? '/api' + loginUser.userAvatar : '')
const avatarFileRef = ref<HTMLInputElement | null>(null)
const uploadingAvatar = ref(false)

const avatarLetter = computed(() => {
  const name = loginUser.userName || loginUser.userAccount || '?'
  return name.charAt(0).toUpperCase()
})

const triggerAvatarUpload = () => {
  avatarFileRef.value?.click()
}

const onAvatarFileChange = async (e: Event) => {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  // 前端校验
  const isImage = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'].includes(file.type)
  if (!isImage) {
    message.error('只支持 jpg/png/gif/webp 格式')
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    message.error('头像不能超过 2MB')
    return
  }

  // 本地预览
  const reader = new FileReader()
  reader.onload = (ev) => {
    avatarPreview.value = ev.target?.result as string
  }
  reader.readAsDataURL(file)

  // 上传
  uploadingAvatar.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const res = await uploadAvatar(formData)
    if (res.data.code === 0) {
      message.success('头像上传成功')
      // 更新 store 中用户头像
      loginUserStore.setLoginUser({ ...loginUser, userAvatar: res.data.data })
    } else {
      message.error('上传失败：' + res.data.message)
    }
  } catch {
    message.error('上传失败，请稍后重试')
  } finally {
    uploadingAvatar.value = false
  }
}

// ===== 修改用户名 =====
const newUserName = ref(loginUser.userName || '')
const savingName = ref(false)

const handleSaveName = async () => {
  if (!newUserName.value || newUserName.value.length > 20) {
    message.warning('用户名不能为空且长度≤20')
    return
  }
  savingName.value = true
  try {
    const res = await updateUsername({ newUserName: newUserName.value })
    if (res.data.code === 0) {
      message.success('用户名修改成功')
      loginUserStore.setLoginUser({ ...loginUser, userName: newUserName.value })
    } else {
      message.error('修改失败：' + res.data.message)
    }
  } catch {
    message.error('请求失败，请稍后重试')
  } finally {
    savingName.value = false
  }
}

// ===== 修改密码 =====
const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})
const savingPwd = ref(false)

const handleSavePassword = async () => {
  if (!passwordForm.value.oldPassword || !passwordForm.value.newPassword) {
    message.warning('密码不能为空')
    return
  }
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    message.warning('两次输入的新密码不一致')
    return
  }
  if (passwordForm.value.newPassword.length < 6 || passwordForm.value.newPassword.length > 20) {
    message.warning('新密码长度须为 6-20 位')
    return
  }
  savingPwd.value = true
  try {
    const res = await updatePassword({
      oldPassword: passwordForm.value.oldPassword,
      newPassword: passwordForm.value.newPassword,
    })
    if (res.data.code === 0) {
      message.success('密码修改成功，请重新登录')
      setTimeout(() => {
        loginUserStore.setLoginUser({ userName: '未登录' })
        router.push('/user/login')
      }, 1500)
    } else {
      message.error('修改失败：' + res.data.message)
    }
  } catch {
    message.error('请求失败，请稍后重试')
  } finally {
    savingPwd.value = false
  }
}
</script>

<template>
  <div class="profile-page">
    <div class="profile-container">
      <h1 class="page-title">个人设置</h1>

      <!-- ===== 头像区域 ===== -->
      <a-card class="profile-card" title="头像">
        <div class="avatar-section">
          <div class="avatar-wrapper" @click="triggerAvatarUpload">
            <a-avatar
              :size="96"
              :src="avatarPreview || undefined"
              class="user-avatar"
            >
              <template v-if="!avatarPreview" #icon>
                <span class="avatar-letter">{{ avatarLetter }}</span>
              </template>
            </a-avatar>
            <div class="avatar-overlay">
              <UploadOutlined />
              <span>更换头像</span>
            </div>
            <a-spin v-if="uploadingAvatar" class="avatar-spin" />
          </div>
          <input
            ref="avatarFileRef"
            type="file"
            accept="image/jpeg,image/png,image/gif,image/webp"
            style="display: none"
            @change="onAvatarFileChange"
          />
          <p class="avatar-hint">支持 jpg/png/gif/webp，最大 2MB，点击头像更换</p>
        </div>
      </a-card>

      <!-- ===== 修改用户名 ===== -->
      <a-card class="profile-card" title="修改用户名">
        <div class="form-row">
          <a-input
            v-model:value="newUserName"
            placeholder="输入新用户名（最多20字）"
            size="large"
            :prefix="(EditOutlined as any)"
            style="flex: 1; margin-right: 12px;"
          />
          <a-button
            type="primary"
            size="large"
            :loading="savingName"
            @click="handleSaveName"
          >保存</a-button>
        </div>
      </a-card>

      <!-- ===== 修改密码 ===== -->
      <a-card class="profile-card" title="修改密码">
        <a-form layout="vertical">
          <a-form-item label="旧密码">
            <a-input-password
              v-model:value="passwordForm.oldPassword"
              placeholder="输入旧密码"
              size="large"
            />
          </a-form-item>
          <a-form-item label="新密码">
            <a-input-password
              v-model:value="passwordForm.newPassword"
              placeholder="输入新密码（6-20位）"
              size="large"
            />
          </a-form-item>
          <a-form-item label="确认新密码">
            <a-input-password
              v-model:value="passwordForm.confirmPassword"
              placeholder="再次输入新密码"
              size="large"
            />
          </a-form-item>
          <a-button
            type="primary"
            size="large"
            :loading="savingPwd"
            @click="handleSavePassword"
            block
          >修改密码</a-button>
        </a-form>
      </a-card>
    </div>
  </div>
</template>

<style scoped>
.profile-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #e8f4fd 0%, #f0f8ff 100%);
  padding: 40px 16px;
}

.profile-container {
  max-width: 560px;
  margin: 0 auto;
}

.page-title {
  font-size: 26px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 24px;
  text-align: center;
}

.profile-card {
  margin-bottom: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(74, 144, 217, 0.1);
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.avatar-wrapper {
  position: relative;
  cursor: pointer;
  border-radius: 50%;
  overflow: hidden;
  width: 96px;
  height: 96px;
}

.avatar-wrapper:hover .avatar-overlay {
  opacity: 1;
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s;
  color: #fff;
  font-size: 12px;
}

.avatar-overlay span {
  font-size: 11px;
}

.avatar-spin {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255,255,255,0.6);
}

.avatar-letter {
  font-size: 36px;
  font-weight: 700;
  color: #fff;
}

.user-avatar {
  background: linear-gradient(135deg, #4A90D9, #7C3AED);
  width: 96px;
  height: 96px;
}

.avatar-hint {
  color: #999;
  font-size: 13px;
  margin: 0;
}

.form-row {
  display: flex;
  align-items: center;
}
</style>
