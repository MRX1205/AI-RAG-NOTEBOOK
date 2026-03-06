<script setup lang="ts">
import { reactive, watch } from 'vue'
import { message } from 'ant-design-vue'
import { saveNote } from '@/api/savedNoteController'

interface Props {
  visible: boolean
  content: string
  sourceQuestion?: string
  notebookId?: string | number
}

const props = withDefaults(defineProps<Props>(), {
  sourceQuestion: '',
})

const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void
  (e: 'success'): void
}>()

const form = reactive({
  title: '',
  content: '',
})

const loading = reactive({ value: false })

// 每次打开弹窗时预填数据
watch(
  () => props.visible,
  (val) => {
    if (val) {
      const q = props.sourceQuestion || ''
      form.title = q.length > 0 ? q.slice(0, 20) : 'AI笔记'
      form.content = props.content
    }
  },
)

const handleOk = async () => {
  if (!form.content.trim()) {
    message.warning('笔记内容不能为空')
    return
  }
  loading.value = true
  try {
    const res = await saveNote({
      notebookId: props.notebookId,
      title: form.title || 'AI笔记',
      content: form.content,
      sourceQuestion: props.sourceQuestion,
    })
    if (res.data.code === 0) {
      message.success('已保存到我的笔记 📌')
      emit('update:visible', false)
      emit('success')
    } else {
      message.error('保存失败：' + res.data.message)
    }
  } catch {
    message.error('网络错误，保存失败')
  } finally {
    loading.value = false
  }
}

const handleCancel = () => {
  emit('update:visible', false)
}
</script>

<template>
  <a-modal
    :open="visible"
    title="📌 保存到我的笔记"
    :confirm-loading="loading.value"
    ok-text="保存"
    cancel-text="取消"
    width="600px"
    @ok="handleOk"
    @cancel="handleCancel"
  >
    <a-form layout="vertical" style="margin-top: 8px;">
      <a-form-item label="笔记标题">
        <a-input
          v-model:value="form.title"
          placeholder="请输入笔记标题"
          :maxlength="100"
          show-count
        />
      </a-form-item>
      <a-form-item label="笔记内容">
        <a-textarea
          v-model:value="form.content"
          :rows="8"
          placeholder="AI回答内容"
        />
      </a-form-item>
      <p v-if="sourceQuestion" style="font-size: 12px; color: #999; margin: 0;">
        来源问题：{{ sourceQuestion }}
      </p>
    </a-form>
  </a-modal>
</template>
