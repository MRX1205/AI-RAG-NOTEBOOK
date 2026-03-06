<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { InboxOutlined } from '@ant-design/icons-vue'
import { uploadSource, addTextSource } from '@/api/sourceController'

/**
 * 来源上传弹窗组件
 *
 * 支持两种来源添加方式：
 * 1. 文件上传：支持 PDF、TXT、Markdown、DOCX
 * 2. 文本输入：手动输入或粘贴文本
 */
const props = defineProps<{
  visible: boolean
  notebookId: string | number
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'success'): void
}>()

/** 当前选中的标签页 */
const activeTab = ref('file')
/** 提交加载状态 */
const submitting = ref(false)
/** 选中的文件列表 */
const fileList = ref<File[]>([])
/** 文本来源表单 */
const textForm = ref({ title: '', content: '' })

/** 选择文件（手动上传模式） */
const handleFileSelect = (info: any) => {
  // 使用 beforeUpload 阻止自动上传，手动管理文件列表
  return false
}

/** 自定义文件选择 */
const beforeUpload = (file: File) => {
  const allowedTypes = ['pdf', 'txt', 'md', 'docx']
  const ext = file.name.split('.').pop()?.toLowerCase() || ''

  if (!allowedTypes.includes(ext)) {
    message.error(`不支持的文件类型：.${ext}，仅支持 PDF、TXT、MD、DOCX`)
    return false
  }

  if (file.size > 20 * 1024 * 1024) {
    message.error('文件大小不能超过 20MB')
    return false
  }

  fileList.value = [file]
  return false // 阻止自动上传
}

/** 提交上传 */
const handleSubmit = async () => {
  if (activeTab.value === 'file') {
    await handleFileUpload()
  } else {
    await handleTextSubmit()
  }
}

/** 上传文件 */
const handleFileUpload = async () => {
  if (fileList.value.length === 0) {
    message.warning('请选择要上传的文件')
    return
  }

  submitting.value = true
  try {
    const res = await uploadSource(props.notebookId, fileList.value[0])
    if (res.data.code === 0) {
      message.success('文件上传成功，正在处理中...')
      resetForm()
      emit('success')
    } else {
      message.error('上传失败：' + res.data.message)
    }
  } catch (error) {
    message.error('网络错误')
  } finally {
    submitting.value = false
  }
}

/** 提交文本来源 */
const handleTextSubmit = async () => {
  if (!textForm.value.title.trim()) {
    message.warning('请输入来源标题')
    return
  }
  if (!textForm.value.content.trim()) {
    message.warning('请输入文本内容')
    return
  }

  submitting.value = true
  try {
    const res = await addTextSource({
      notebookId: props.notebookId,
      title: textForm.value.title,
      content: textForm.value.content,
    })
    if (res.data.code === 0) {
      message.success('文本来源添加成功')
      resetForm()
      emit('success')
    } else {
      message.error('添加失败：' + res.data.message)
    }
  } catch (error) {
    message.error('网络错误')
  } finally {
    submitting.value = false
  }
}

/** 重置表单 */
const resetForm = () => {
  fileList.value = []
  textForm.value = { title: '', content: '' }
}

/** 关闭弹窗 */
const handleCancel = () => {
  resetForm()
  emit('update:visible', false)
}
</script>

<template>
  <a-modal
    :open="props.visible"
    title="添加来源"
    :width="560"
    ok-text="添加"
    cancel-text="取消"
    :confirm-loading="submitting"
    @ok="handleSubmit"
    @cancel="handleCancel"
  >
    <a-tabs v-model:activeKey="activeTab" style="margin-top: 8px">
      <!-- 文件上传 Tab -->
      <a-tab-pane key="file" tab="上传文件">
        <a-upload-dragger
          :before-upload="beforeUpload"
          :file-list="[]"
          :show-upload-list="false"
          accept=".pdf,.txt,.md,.docx"
        >
          <p class="ant-upload-drag-icon">
            <InboxOutlined />
          </p>
          <p class="ant-upload-text">点击或拖拽文件到此区域</p>
          <p class="ant-upload-hint">支持 PDF、TXT、Markdown、Word 文件，单文件最大 20MB</p>
        </a-upload-dragger>
        <div v-if="fileList.length > 0" class="selected-file">
          <a-tag color="blue" closable @close="fileList = []">
            {{ fileList[0].name }} ({{ (fileList[0].size / 1024).toFixed(1) }} KB)
          </a-tag>
        </div>
      </a-tab-pane>

      <!-- 文本输入 Tab -->
      <a-tab-pane key="text" tab="粘贴文本">
        <a-form layout="vertical">
          <a-form-item label="来源标题" required>
            <a-input
              v-model:value="textForm.title"
              placeholder="输入来源名称"
              :maxlength="100"
            />
          </a-form-item>
          <a-form-item label="文本内容" required>
            <a-textarea
              v-model:value="textForm.content"
              placeholder="粘贴或输入文本内容..."
              :rows="8"
            />
          </a-form-item>
        </a-form>
      </a-tab-pane>
    </a-tabs>
  </a-modal>
</template>

<style scoped>
.selected-file {
  margin-top: 12px;
}
</style>
