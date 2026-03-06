<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import {
  SendOutlined,
  UserOutlined,
  CopyOutlined,
  LikeOutlined,
  DislikeOutlined,
} from '@ant-design/icons-vue'
import { getChatStreamUrl, getChatSuggestions } from '@/api/chatController'
import SaveNoteDialog from './SaveNoteDialog.vue'

const props = defineProps<{
  notebookId: string | number
  selectedSourceIds: (string | number)[]
}>()

interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  loading?: boolean
}

const messages = ref<ChatMessage[]>([])
const inputText = ref('')
const isGenerating = ref(false)
const suggestions = ref<string[]>([])
const messageContainer = ref<HTMLElement | null>(null)

onMounted(() => {
  loadSuggestions()
})

const loadSuggestions = async () => {
  try {
    const res = await getChatSuggestions(props.notebookId)
    if (res.data.code === 0 && res.data.data) {
      suggestions.value = res.data.data
    }
  } catch (error) {}
}

const sendMessage = async (text?: string) => {
  const msg = text || inputText.value.trim()
  if (!msg || isGenerating.value) return
  inputText.value = ''
  suggestions.value = []
  messages.value.push({ role: 'user', content: msg })
  messages.value.push({ role: 'assistant', content: '', loading: true })
  isGenerating.value = true
  scrollToBottom()
  const url = getChatStreamUrl(props.notebookId, msg, props.selectedSourceIds)
  const eventSource = new EventSource(url)
  const aiMessageIndex = messages.value.length - 1
  eventSource.onmessage = (event) => {
    const data = event.data
    if (data === '[DONE]') {
      eventSource.close()
      messages.value[aiMessageIndex].loading = false
      isGenerating.value = false
      loadSuggestions()
      return
    }
    if (data === '[ERROR]') {
      eventSource.close()
      messages.value[aiMessageIndex].content = '抱歉，AI 服务暂时不可用，请稍后重试。'
      messages.value[aiMessageIndex].loading = false
      isGenerating.value = false
      return
    }
    messages.value[aiMessageIndex].content += data
    scrollToBottom()
  }
  eventSource.onerror = () => {
    eventSource.close()
    if (messages.value[aiMessageIndex].loading) {
      messages.value[aiMessageIndex].content += '\n\n（连接中断）'
      messages.value[aiMessageIndex].loading = false
      isGenerating.value = false
    }
  }
}

const clickSuggestion = (text: string) => sendMessage(text)

const scrollToBottom = () => {
  nextTick(() => {
    if (messageContainer.value) {
      messageContainer.value.scrollTop = messageContainer.value.scrollHeight
    }
  })
}

const handleKeyDown = (e: KeyboardEvent) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}

const copyContent = (content: string) => {
  navigator.clipboard.writeText(content)
  message.success('已复制')
}

// 保存到笔记相关状态
const saveNoteDialogVisible = ref(false)
const saveNoteContent = ref('')
const saveNoteQuestion = ref('')

const saveToNote = (msgIndex: number) => {
  const aiMsg = messages.value[msgIndex]
  if (!aiMsg) return
  saveNoteContent.value = aiMsg.content
  // 找上一条用户消息作为问题
  for (let i = msgIndex - 1; i >= 0; i--) {
    if (messages.value[i].role === 'user') {
      saveNoteQuestion.value = messages.value[i].content
      break
    }
  }
  saveNoteDialogVisible.value = true
}
</script>

<template>
  <div class="chat-panel">
    <!-- 面板标题栏 -->
    <div class="panel-header">
      <h3 class="panel-title">对话</h3>
    </div>

    <!-- 消息列表 -->
    <div class="message-list" ref="messageContainer">
      <!-- 欢迎消息 -->
      <div v-if="messages.length === 0" class="welcome-area">
        <div class="welcome-icon">🤖</div>
        <h3 class="welcome-title">AI 知识助手</h3>
        <p class="welcome-desc">基于您上传的来源文档进行 AI 对话，回答将严格引用文档内容。</p>

        <div v-if="suggestions.length > 0" class="suggestions">
          <div
            v-for="(suggestion, index) in suggestions"
            :key="index"
            class="suggestion-item"
            @click="clickSuggestion(suggestion)"
          >
            {{ suggestion }}
          </div>
        </div>
      </div>

      <!-- 消息气泡 -->
      <div
        v-for="(msg, index) in messages"
        :key="index"
        class="message-item"
        :class="msg.role"
      >
        <!-- AI 消息 -->
        <template v-if="msg.role === 'assistant'">
          <div class="ai-message-content">
            <div class="message-text" v-html="renderMarkdown(msg.content)"></div>
            <div v-if="msg.loading" class="typing-indicator">
              <span></span><span></span><span></span>
            </div>
            <!-- 消息操作按钮（NotebookLM 风格） -->
            <div v-if="!msg.loading && msg.content" class="message-actions">
              <button class="action-btn" @click="saveToNote(index)" title="保存到笔记">
                📌 保存到笔记
              </button>
              <button class="action-btn icon-only" @click="copyContent(msg.content)" title="复制">
                <CopyOutlined />
              </button>
              <button class="action-btn icon-only" title="有帮助">
                <LikeOutlined />
              </button>
              <button class="action-btn icon-only" title="没有帮助">
                <DislikeOutlined />
              </button>
            </div>
          </div>
        </template>

        <!-- 用户消息 -->
        <template v-else>
          <div class="user-message-content">
            {{ msg.content }}
          </div>
        </template>
      </div>

      <!-- 后续推荐问题 -->
      <div v-if="suggestions.length > 0 && messages.length > 0 && !isGenerating" class="follow-up-suggestions">
        <div
          v-for="(suggestion, index) in suggestions"
          :key="index"
          class="suggestion-chip"
          @click="clickSuggestion(suggestion)"
        >
          {{ suggestion }}
        </div>
      </div>
    </div>

    <!-- 输入区域 — NotebookLM 风格 -->
    <div class="input-area">
      <div class="input-wrapper">
        <textarea
          v-model="inputText"
          placeholder="开始输入..."
          :disabled="isGenerating"
          @keydown="handleKeyDown"
          rows="1"
          class="chat-input"
        ></textarea>
        <span class="source-count-tag">{{ props.selectedSourceIds.length || 0 }} 个来源</span>
        <button
          class="send-btn"
          :disabled="!inputText.trim() || isGenerating"
          @click="sendMessage()"
        >
          <SendOutlined />
        </button>
      </div>
    </div>
  </div>

  <!-- 保存笔记弹窗 -->
  <SaveNoteDialog
    v-model:visible="saveNoteDialogVisible"
    :content="saveNoteContent"
    :source-question="saveNoteQuestion"
    :notebook-id="props.notebookId"
  />
</template>

<script lang="ts">
function renderMarkdown(text: string): string {
  if (!text) return ''
  return text
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
    .replace(/^### (.*$)/gm, '<h4>$1</h4>')
    .replace(/^## (.*$)/gm, '<h3>$1</h3>')
    .replace(/^# (.*$)/gm, '<h2>$1</h2>')
    .replace(/^- (.*$)/gm, '<li>$1</li>')
    .replace(/\[(\d+)\]/g, '<sup class="citation">[$1]</sup>')
    .replace(/\n/g, '<br>')
}
</script>

<style scoped>
.chat-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: transparent;
}

.panel-header {
  padding: 14px 20px;
  background: #fff;
  border-bottom: 1px solid var(--border-color);
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0;
  color: var(--primary-color);
}

/* ========== 消息列表 ========== */
.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.welcome-area {
  text-align: center;
  padding: 80px 20px 40px;
}

.welcome-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.welcome-title {
  font-size: 22px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 8px;
}

.welcome-desc {
  color: #999;
  font-size: 14px;
  margin-bottom: 32px;
}

.suggestions {
  max-width: 500px;
  margin: 0 auto;
  text-align: left;
}

.suggestion-item {
  padding: 12px 16px;
  background: #fff;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 14px;
  color: #3c4043;
}

.suggestion-item:hover {
  background: var(--bg-block-light);
  border-color: var(--primary-color);
  color: var(--primary-color);
}

/* ========== 消息气泡 ========== */
.message-item {
  margin-bottom: 20px;
}

.message-item.user {
  display: flex;
  justify-content: flex-end;
}

.user-message-content {
  max-width: 70%;
  padding: 12px 16px;
  background: var(--bg-color-gradient);
  color: var(--primary-color);
  border: 1px solid var(--border-color);
  box-shadow: 0 1px 2px rgba(74, 144, 217, 0.05);
  border-radius: 18px 18px 4px 18px;
  font-size: 14px;
  line-height: 1.6;
}

.ai-message-content {
  max-width: 85%;
  padding: 16px 20px;
  background: #fff;
  border: 1px solid var(--border-color);
  border-radius: 4px 18px 18px 18px;
  font-size: 14px;
  line-height: 1.8;
  color: #1a1a1a;
  box-shadow: 0 1px 2px rgba(74, 144, 217, 0.05);
}

.message-text :deep(h2),
.message-text :deep(h3),
.message-text :deep(h4) {
  margin: 12px 0 8px;
  color: var(--primary-color);
}

.message-text :deep(.citation) {
  color: var(--primary-color);
  font-weight: 600;
  cursor: pointer;
}

/* 消息操作按钮 — NotebookLM 风格 */
.message-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #f0f0f0;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: none;
  border: 1px solid var(--border-color);
  border-radius: 16px;
  cursor: pointer;
  font-size: 12px;
  color: #5f6368;
  transition: all 0.15s;
}

.action-btn:hover {
  background: var(--bg-block-light);
  border-color: var(--border-color);
  color: var(--primary-hover);
}

.action-btn.icon-only {
  width: 28px;
  height: 28px;
  padding: 0;
  justify-content: center;
  border-radius: 50%;
}

/* 打字指示器 */
.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 4px 0;
}

.typing-indicator span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #999;
  animation: typing 1.4s infinite ease-in-out;
}

.typing-indicator span:nth-child(2) { animation-delay: 0.2s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.4s; }

@keyframes typing {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
  40% { transform: scale(1); opacity: 1; }
}

/* 推荐问题 */
.follow-up-suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.suggestion-chip {
  padding: 6px 14px;
  background: #fff;
  border: 1px solid var(--border-color);
  border-radius: 16px;
  font-size: 13px;
  color: #3c4043;
  cursor: pointer;
  transition: all 0.2s;
}

.suggestion-chip:hover {
  background: var(--bg-block-light);
  border-color: var(--primary-hover);
  color: var(--primary-hover);
}

/* ========== 输入区域 ========== */
.input-area {
  padding: 12px 24px 16px;
  background: transparent;
}

.input-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #fff;
  border: 1px solid var(--border-color);
  border-radius: 24px;
  padding: 6px 8px 6px 18px;
  transition: all 0.2s;
}

.input-wrapper:focus-within {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px rgba(74, 144, 217, 0.1);
}

.chat-input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 14px;
  resize: none;
  background: transparent;
  line-height: 1.5;
  padding: 6px 0;
  font-family: inherit;
}

.source-count-tag {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
  padding: 2px 8px;
  background: #f5f5f5;
  border-radius: 10px;
}

.send-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: none;
  background: var(--primary-color);
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
  transition: all 0.2s;
}

.send-btn:hover:not(:disabled) {
  background: var(--primary-hover);
}

.send-btn:disabled {
  background: #d9d9d9;
  cursor: not-allowed;
}
</style>
