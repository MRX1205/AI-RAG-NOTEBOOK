<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { addNotebook } from '@/api/notebookController'

/**
 * 创建笔记本弹窗组件
 *
 * 使用 Ant Design Vue 的 Modal 组件实现。
 * 通过 props 控制弹窗显示/隐藏，通过 emit 通知父组件操作结果。
 *
 * 组件通信方式：
 * - props.visible：父组件传入，控制弹窗是否显示
 * - emit('update:visible')：通知父组件更新 visible 状态（实现 v-model 双向绑定）
 * - emit('success')：创建成功后通知父组件刷新列表
 */

// ================== Props 和 Emits ==================

/**
 * defineProps：声明组件接收的属性
 * defineEmits：声明组件可以触发的事件
 */
const props = defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'success'): void
}>()

// ================== 响应式数据 ==================

/** 表单数据 */
const formData = ref<API.NotebookAddRequest>({
  title: '',
  description: '',
})

/** 提交加载状态 */
const submitting = ref(false)

// ================== 方法 ==================

/**
 * 提交创建笔记本
 * 调用后端 POST /notebook/add 接口
 */
const handleSubmit = async () => {
  // 1. 前端参数校验
  if (!formData.value.title || formData.value.title.trim() === '') {
    message.warning('请输入笔记本标题')
    return
  }

  submitting.value = true
  try {
    // 2. 调用后端接口
    const res = await addNotebook(formData.value)
    if (res.data.code === 0) {
      message.success('创建成功')
      // 3. 重置表单
      formData.value = { title: '', description: '' }
      // 4. 通知父组件创建成功（父组件会关闭弹窗并刷新列表）
      emit('success')
    } else {
      message.error('创建失败：' + res.data.message)
    }
  } catch (error) {
    message.error('网络错误，请稍后重试')
  } finally {
    submitting.value = false
  }
}

/**
 * 关闭弹窗
 * 通过 emit 通知父组件更新 visible 状态
 */
const handleCancel = () => {
  formData.value = { title: '', description: '' }
  emit('update:visible', false)
}
</script>

<template>
  <!--
    a-modal：Ant Design Vue 的模态框组件
    :open 控制显示/隐藏
    @cancel 关闭回调
    :confirmLoading 确认按钮加载状态
    @ok 确认按钮点击回调
  -->
  <a-modal
    :open="props.visible"
    title="创建笔记本"
    ok-text="创建"
    cancel-text="取消"
    :confirm-loading="submitting"
    @ok="handleSubmit"
    @cancel="handleCancel"
  >
    <a-form layout="vertical" style="margin-top: 16px">
      <a-form-item label="笔记本标题" required>
        <a-input
          v-model:value="formData.title"
          placeholder="输入笔记本标题，例如：RAG 知识库学习"
          :maxlength="100"
          show-count
        />
      </a-form-item>
      <a-form-item label="描述（可选）">
        <a-textarea
          v-model:value="formData.description"
          placeholder="简单描述这个笔记本的用途..."
          :rows="3"
          :maxlength="500"
          show-count
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>
