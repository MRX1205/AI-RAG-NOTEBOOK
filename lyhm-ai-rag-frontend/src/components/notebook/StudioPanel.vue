<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  FileTextOutlined,
  FormOutlined,
  PlusOutlined,
  DeleteOutlined,
  EditOutlined,
  PushpinOutlined,
  DownOutlined,
  UpOutlined,
} from '@ant-design/icons-vue'
import { listReports, deleteReport } from '@/api/reportController'
import { listQuizzes, deleteQuiz, generateQuiz } from '@/api/quizController'
import { listMyNotes, deleteNote } from '@/api/savedNoteController'

/**
 * Studio 面板（右侧栏）— 对齐 NotebookLM 风格
 *
 * 顶部：5×2 彩色工具卡片网格
 * 底部：已生成的报告和测验列表
 */
const props = defineProps<{
  notebookId: string | number
  selectedSourceIds: (string | number)[]
}>()

/** 报告列表 */
const reports = ref<API.ReportVO[]>([])
/** 测验列表 */
const quizzes = ref<API.QuizVO[]>([])
const loadingReports = ref(false)
const loadingQuizzes = ref(false)

/** 报告类型选择弹窗 */
const reportDialogVisible = ref(false)
const selectedReportType = ref('briefing')
const customPrompt = ref('')
const generatingReport = ref(false)

/** 测验配置弹窗 */
const quizDialogVisible = ref(false)
const quizConfig = ref({ questionCount: 10, difficulty: 'medium' })
const generatingQuiz = ref(false)

/** 报告查看弹窗 */
const reportViewerVisible = ref(false)
const currentReport = ref<API.ReportVO | null>(null)

/** 测验答题弹窗 */
const quizViewerVisible = ref(false)
const currentQuiz = ref<API.QuizVO | null>(null)
const quizAnswers = ref<Record<number, string>>({})
const quizSubmitted = ref(false)

/** Studio 工具卡片定义 */
const studioTools = [
  { id: 'audio',     icon: '🎧', name: '音频概览', color: '#fef3e2', available: false },
  { id: 'slides',    icon: '🖥️', name: '演示文稿', color: '#f3e8f9', available: false },
  { id: 'video',     icon: '🎬', name: '视频概览', color: '#e8f5e9', available: false },
  { id: 'mindmap',   icon: '🧠', name: '思维导图', color: '#e3f2fd', available: false },
  { id: 'report',    icon: '📋', name: '报告',     color: '#fff3e0', available: true  },
  { id: 'flashcard', icon: '🃏', name: '闪卡',     color: '#fce4ec', available: false },
  { id: 'quiz',      icon: '📝', name: '测验',     color: '#e8eaf6', available: true  },
  { id: 'info',      icon: '📊', name: '信息图',   color: '#e0f2f1', available: false },
  { id: 'table',     icon: '📑', name: '数据表格', color: '#f3e5f5', available: false },
]

const reportTypeOptions = [
  { value: 'briefing', label: '📋 简报文档' },
  { value: 'study_guide', label: '📖 学习指南' },
  { value: 'faq', label: '❓ 常见问题' },
  { value: 'timeline', label: '⏳ 时间线' },
  { value: 'custom', label: '✏️ 自定义' },
]

// ===== 已保存笔记区域 =====
/** 当前笔记本的已保存笔记列表 */
const savedNotes = ref<API.SavedNoteVO[]>([])
const loadingNotes = ref(false)
/** 已保存笔记区域是否折叠（默认展开） */
const notesCollapsed = ref(false)

/** 加载当前笔记本相关的保存笔记 */
const loadSavedNotes = async () => {
  loadingNotes.value = true
  try {
    const res = await listMyNotes({ notebookId: props.notebookId, pageNum: 1, pageSize: 50 })
    if (res.data.code === 0 && res.data.data) {
      savedNotes.value = res.data.data.records ?? []
    }
  } catch {
    // 未登录时静默处理
  } finally {
    loadingNotes.value = false
  }
}

/** 删除保存的笔记 */
const handleDeleteSavedNote = async (id: string | number) => {
  try {
    const res = await deleteNote(id)
    if (res.data.code === 0) {
      message.success('已删除')
      loadSavedNotes()
    }
  } catch {
    message.error('删除失败')
  }
}

/** 展开某条笔记查看全文 */
const expandedNoteId = ref<string | number | null>(null)
const toggleNoteExpand = (id: string | number) => {
  expandedNoteId.value = expandedNoteId.value === id ? null : id
}

onMounted(() => {
  loadReports()
  loadQuizzes()
  loadSavedNotes()
})

/** 供父组件（ChatPanel）调用，保存笔记后刷新列表 */
defineExpose({ loadSavedNotes })

/** 点击工具卡片 */
const handleToolClick = (tool: typeof studioTools[0]) => {
  if (!tool.available) {
    message.info(`${tool.name} 功能即将推出`)
    return
  }
  if (tool.id === 'report') {
    reportDialogVisible.value = true
  } else if (tool.id === 'quiz') {
    quizDialogVisible.value = true
  }
}

/** 加载报告列表 */
const loadReports = async () => {
  loadingReports.value = true
  try {
    const res = await listReports(props.notebookId)
    if (res.data.code === 0) {
      reports.value = res.data.data || []
    }
  } catch{}
  finally { loadingReports.value = false }
}

/** 加载测验列表 */
const loadQuizzes = async () => {
  loadingQuizzes.value = true
  try {
    const res = await listQuizzes(props.notebookId)
    if (res.data.code === 0) {
      quizzes.value = res.data.data || []
    }
  } catch{}
  finally { loadingQuizzes.value = false }
}

/** 生成报告（使用 SSE） */
const handleGenerateReport = async () => {
  generatingReport.value = true
  const body: API.ReportGenerateRequest = {
    notebookId: props.notebookId,
    reportType: selectedReportType.value,
    sourceIds: props.selectedSourceIds,
    customPrompt: selectedReportType.value === 'custom' ? customPrompt.value : undefined,
  }

  try {
    const response = await fetch(`${window.location.origin}/api/report/generate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
      credentials: 'include',
    })

    if (!response.ok) throw new Error('报告生成请求失败')

    const reader = response.body!.getReader()
    const decoder = new TextDecoder()
    let reportContent = ''

    currentReport.value = {
      title: getReportTypeLabel(selectedReportType.value),
      reportType: selectedReportType.value,
      content: '',
    }
    reportViewerVisible.value = true
    reportDialogVisible.value = false

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      const text = decoder.decode(value, { stream: true })
      const lines = text.split('\n')
      for (const line of lines) {
        if (line.startsWith('data:')) {
          const data = line.substring(5).trim()
          if (data.startsWith('[DONE')) break
          else if (data === '[ERROR]') {
            message.error('报告生成失败')
            break
          } else {
            reportContent += data
            currentReport.value.content = reportContent
          }
        }
      }
    }

    message.success('报告生成完成')
    loadReports()
  } catch (error) {
    message.error('报告生成失败')
  } finally {
    generatingReport.value = false
  }
}

/** 生成测验 */
const handleGenerateQuiz = async () => {
  generatingQuiz.value = true
  try {
    const res = await generateQuiz({
      notebookId: props.notebookId,
      questionCount: quizConfig.value.questionCount,
      difficulty: quizConfig.value.difficulty,
      sourceIds: props.selectedSourceIds,
    })
    if (res.data.code === 0) {
      message.success('测验生成成功')
      quizDialogVisible.value = false
      loadQuizzes()
      if (res.data.data) openQuiz(res.data.data)
    } else {
      message.error('测验生成失败：' + res.data.message)
    }
  } catch (error) {
    message.error('网络错误')
  } finally {
    generatingQuiz.value = false
  }
}

const openQuiz = (quiz: API.QuizVO) => {
  currentQuiz.value = quiz
  quizAnswers.value = {}
  quizSubmitted.value = false
  quizViewerVisible.value = true
}

const viewReport = (report: API.ReportVO) => {
  currentReport.value = report
  reportViewerVisible.value = true
}

const handleDeleteReport = async (id: string | number) => {
  const res = await deleteReport({ id })
  if (res.data.code === 0) {
    message.success('删除成功')
    loadReports()
  }
}

const handleDeleteQuiz = async (id: string | number) => {
  const res = await deleteQuiz({ id })
  if (res.data.code === 0) {
    message.success('删除成功')
    loadQuizzes()
  }
}

const getReportTypeLabel = (type: string) => {
  const option = reportTypeOptions.find(o => o.value === type)
  return option ? option.label : type
}

const getDifficultyLabel = (d: string) => {
  switch (d) {
    case 'easy': return '简单'
    case 'medium': return '中等'
    case 'hard': return '困难'
    default: return d
  }
}

const parseQuestions = (questionsJson?: string): API.QuizQuestion[] => {
  if (!questionsJson) return []
  try { return JSON.parse(questionsJson) }
  catch { return [] }
}

const calculateScore = (): number => {
  if (!currentQuiz.value?.questions) return 0
  try {
    const questions: API.QuizQuestion[] = JSON.parse(currentQuiz.value.questions)
    if (questions.length === 0) return 0
    let correct = 0
    for (const q of questions) {
      if (q.questionId && quizAnswers.value[q.questionId] === q.correctAnswer) correct++
    }
    return Math.round((correct / questions.length) * 100)
  } catch { return 0 }
}
</script>

<template>
  <div class="studio-panel">
    <!-- 面板标题 -->
    <div class="panel-header">
      <h3 class="panel-title">Studio</h3>
    </div>

    <!-- 工具卡片网格 — 对齐 NotebookLM 5×2 网格 -->
    <div class="tools-grid">
      <div
        v-for="tool in studioTools"
        :key="tool.id"
        class="tool-card"
        :class="{ disabled: !tool.available }"
        :style="{ background: tool.color }"
        @click="handleToolClick(tool)"
      >
        <div class="tool-icon">{{ tool.icon }}</div>
        <span class="tool-name">{{ tool.name }}</span>
        <span v-if="!tool.available" class="tool-badge">即将推出</span>
        <button v-if="tool.available" class="tool-edit-btn">
          <EditOutlined />
        </button>
      </div>
    </div>

    <!-- 已生成内容列表 -->
    <div class="generated-content" v-if="reports.length > 0 || quizzes.length > 0">
      <a-divider style="margin: 8px 0 12px" />

      <!-- 报告列表 -->
      <div v-for="report in reports" :key="'r-' + report.id" class="content-item" @click="viewReport(report)">
        <div class="content-item-icon">📋</div>
        <div class="content-item-info">
          <div class="content-item-title">{{ report.title }}</div>
          <div class="content-item-meta">{{ report.createTime?.substring(0, 10) }}</div>
        </div>
        <div class="content-item-actions" @click.stop>
          <a-popconfirm title="确定删除？" @confirm="handleDeleteReport(report.id!)">
            <a-button type="text" danger size="small"><template #icon><DeleteOutlined /></template></a-button>
          </a-popconfirm>
        </div>
      </div>

      <!-- 测验列表 -->
      <div v-for="quiz in quizzes" :key="'q-' + quiz.id" class="content-item" @click="openQuiz(quiz)">
        <div class="content-item-icon">📝</div>
        <div class="content-item-info">
          <div class="content-item-title">{{ quiz.title }}</div>
          <div class="content-item-meta">{{ quiz.questionCount }} 题 · {{ getDifficultyLabel(quiz.difficulty || '') }}</div>
        </div>
        <div class="content-item-actions" @click.stop>
          <a-popconfirm title="确定删除？" @confirm="handleDeleteQuiz(quiz.id!)">
            <a-button type="text" danger size="small"><template #icon><DeleteOutlined /></template></a-button>
          </a-popconfirm>
        </div>
      </div>
    </div>

    <!-- 空状态提示 -->
    <div v-else class="studio-empty">
      <div class="studio-empty-icon">✨</div>
      <div class="studio-empty-text">Studio 输出将保存在此处。</div>
      <div class="studio-empty-hint">添加来源后，点击即可添加音频概览、学习指南、思维导图等！</div>
    </div>

    <!-- ===== 已保存笔记区域 ===== -->
    <div class="saved-notes-section">
      <a-divider style="margin: 12px 0 0" />
      <!-- 标题栏（点击折叠/展开） -->
      <div class="notes-header" @click="notesCollapsed = !notesCollapsed">
        <span class="notes-title">
          <PushpinOutlined style="margin-right: 6px;" />
          已保存笔记
          <a-badge
            v-if="savedNotes.length > 0"
            :count="savedNotes.length"
            :overflow-count="99"
            style="margin-left: 6px;"
          />
        </span>
        <UpOutlined v-if="!notesCollapsed" style="font-size: 12px; color: #999;" />
        <DownOutlined v-else style="font-size: 12px; color: #999;" />
      </div>

      <!-- 笔记列表（可折叠） -->
      <div v-show="!notesCollapsed" class="notes-body">
        <!-- 加载中 -->
        <div v-if="loadingNotes" style="text-align:center; padding: 12px 0;">
          <a-spin size="small" />
        </div>

        <!-- 空状态 -->
        <div v-else-if="savedNotes.length === 0" class="notes-empty">
          <p>还没有保存笔记</p>
          <p class="notes-empty-hint">在对话中点击「📌 保存到笔记」即可</p>
        </div>

        <!-- 笔记卡片列表 -->
        <div v-else>
          <div
            v-for="note in savedNotes"
            :key="String(note.id)"
            class="note-item"
          >
            <div class="note-item-header">
              <span class="note-item-title" @click="toggleNoteExpand(note.id!)">
                {{ note.title || 'AI笔记' }}
              </span>
              <a-popconfirm
                title="确定删除这条笔记？"
                ok-text="确认"
                cancel-text="取消"
                @confirm="handleDeleteSavedNote(note.id!)"
              >
                <a-button type="text" size="small" danger style="flex-shrink:0; padding: 0 4px;">
                  <template #icon><DeleteOutlined /></template>
                </a-button>
              </a-popconfirm>
            </div>
            <!-- 折叠时显示前50字摘要，展开时显示全文 -->
            <div
              class="note-item-content"
              :class="{ expanded: expandedNoteId === note.id }"
              @click="toggleNoteExpand(note.id!)"
            >
              {{ note.content }}
            </div>
            <div v-if="note.sourceQuestion" class="note-item-question">
              Q: {{ note.sourceQuestion }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 报告类型选择弹窗 -->
    <a-modal v-model:open="reportDialogVisible" title="选择报告类型" ok-text="生成" :confirm-loading="generatingReport" @ok="handleGenerateReport">
      <a-radio-group v-model:value="selectedReportType" class="report-type-group">
        <a-radio-button v-for="opt in reportTypeOptions" :key="opt.value" :value="opt.value" class="report-type-btn">
          {{ opt.label }}
        </a-radio-button>
      </a-radio-group>
      <a-textarea v-if="selectedReportType === 'custom'" v-model:value="customPrompt" placeholder="输入自定义报告指令..." :rows="3" style="margin-top: 12px" />
    </a-modal>

    <!-- 报告查看弹窗 -->
    <a-modal v-model:open="reportViewerVisible" :title="currentReport?.title || '报告'" :width="720" :footer="null">
      <div class="report-content" v-html="renderMarkdown(currentReport?.content || '')"></div>
    </a-modal>

    <!-- 测验配置弹窗 -->
    <a-modal v-model:open="quizDialogVisible" title="生成测验" ok-text="生成" :confirm-loading="generatingQuiz" @ok="handleGenerateQuiz">
      <a-form layout="vertical" style="margin-top: 12px">
        <a-form-item label="题目数量">
          <a-radio-group v-model:value="quizConfig.questionCount">
            <a-radio-button :value="5">5 题</a-radio-button>
            <a-radio-button :value="10">10 题</a-radio-button>
            <a-radio-button :value="15">15 题</a-radio-button>
            <a-radio-button :value="20">20 题</a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="难度级别">
          <a-radio-group v-model:value="quizConfig.difficulty">
            <a-radio-button value="easy">简单</a-radio-button>
            <a-radio-button value="medium">中等</a-radio-button>
            <a-radio-button value="hard">困难</a-radio-button>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 测验答题弹窗 -->
    <a-modal v-model:open="quizViewerVisible" :title="currentQuiz?.title || '测验'" :width="720" :footer="null">
      <div v-if="currentQuiz" class="quiz-content">
        <div v-for="(q, qIdx) in parseQuestions(currentQuiz.questions)" :key="qIdx" class="quiz-question">
          <div class="question-text">{{ q.questionId }}. {{ q.question }}</div>
          <a-radio-group v-model:value="quizAnswers[q.questionId!]" class="options-group">
            <div v-for="opt in q.options" :key="opt.label" class="option-item"
                 :class="{
                   correct: quizSubmitted && opt.label === q.correctAnswer,
                   wrong: quizSubmitted && quizAnswers[q.questionId!] === opt.label && opt.label !== q.correctAnswer
                 }">
              <a-radio :value="opt.label" :disabled="quizSubmitted">
                {{ opt.label }}. {{ opt.text }}
              </a-radio>
            </div>
          </a-radio-group>
          <div v-if="quizSubmitted" class="explanation">
            <strong>解析：</strong>{{ q.explanation }}
          </div>
        </div>
        <div class="quiz-footer">
          <a-button v-if="!quizSubmitted" type="primary" @click="quizSubmitted = true" :disabled="Object.keys(quizAnswers).length === 0">
            提交答案
          </a-button>
          <div v-if="quizSubmitted" class="score-display">
            得分：{{ calculateScore() }} / 100
          </div>
        </div>
      </div>
    </a-modal>
  </div>
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
    .replace(/\[(\d+)\]/g, '<sup style="color:#1890ff;font-weight:600">[$1]</sup>')
    .replace(/\n/g, '<br>')
}
</script>

<style scoped>
.studio-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.panel-header {
  padding: 14px 16px;
  border-bottom: 1px solid var(--border-color);
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0;
  color: var(--primary-color);
}

/* ========== 工具卡片网格 ========== */
.tools-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  padding: 12px;
}

.tool-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 16px 8px;
  border-radius: var(--border-radius);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  min-height: 72px;
  border: 1px solid transparent;
}

.tool-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--box-shadow-hover);
  border-color: var(--primary-hover);
}

.tool-card.disabled {
  opacity: 0.65;
  cursor: default;
}

.tool-card.disabled:hover {
  transform: none;
  box-shadow: none;
  border-color: transparent;
}

.tool-icon {
  font-size: 22px;
  margin-bottom: 4px;
}

.tool-name {
  font-size: 12px;
  font-weight: 500;
  color: #3c4043;
}

.tool-badge {
  position: absolute;
  top: 4px;
  right: 4px;
  font-size: 9px;
  color: #999;
  background: rgba(255,255,255,0.7);
  padding: 1px 4px;
  border-radius: 4px;
}

.tool-edit-btn {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: none;
  background: rgba(255,255,255,0.7);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #666;
  opacity: 0;
  transition: opacity 0.2s;
}

.tool-card:hover .tool-edit-btn {
  opacity: 1;
}

/* ========== 已生成内容列表 ========== */
.generated-content {
  flex: 1;
  overflow-y: auto;
  padding: 0 12px;
}

.content-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: var(--border-radius);
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 4px; border: 1px solid transparent;
}

.content-item:hover {
  background: var(--bg-block-light);
  border-color: var(--border-color);
}

.content-item-icon {
  font-size: 20px;
  flex-shrink: 0;
}

.content-item-info {
  flex: 1;
  min-width: 0;
}

.content-item-title {
  font-size: 13px;
  font-weight: 500;
  color: #1a1a1a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.content-item-meta {
  font-size: 11px;
  color: #999;
  margin-top: 2px;
}

.content-item-actions {
  opacity: 0;
  transition: opacity 0.2s;
}

.content-item:hover .content-item-actions {
  opacity: 1;
}

/* ========== 空状态 ========== */
.studio-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 24px;
  text-align: center;
}

.studio-empty-icon {
  font-size: 36px;
  margin-bottom: 12px;
}

.studio-empty-text {
  font-size: 14px;
  font-weight: 500;
  color: #5f6368;
  margin-bottom: 8px;
}

.studio-empty-hint {
  font-size: 13px;
  color: #999;
  line-height: 1.5;
}

/* ========== 底部 ========== */
.studio-footer {
  padding: 12px 16px;
  border-top: 1px solid var(--border-color);
}

/* ========== 弹窗内容样式 ========== */
.report-type-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
}

.report-type-btn {
  text-align: left !important;
  height: auto !important;
  padding: 10px 16px !important;
}

.report-content {
  max-height: 60vh;
  overflow-y: auto;
  line-height: 1.8;
  font-size: 14px;
}

.quiz-question {
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.question-text {
  font-size: 15px;
  font-weight: 500;
  margin-bottom: 12px;
  color: #1a1a1a;
}

.options-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.option-item {
  padding: 8px 12px;
  border-radius: 6px;
  transition: background 0.2s;
}

.option-item:hover { background: #f5f5f5; }
.option-item.correct { background: #f6ffed; border: 1px solid #52c41a; }
.option-item.wrong { background: #fff2f0; border: 1px solid #ff4d4f; }

.explanation {
  margin-top: 8px;
  padding: 8px 12px;
  background: #e6f7ff;
  border-radius: 6px;
  font-size: 13px;
  color: #333;
}

.quiz-footer {
  display: flex;
  justify-content: center;
  padding: 16px 0;
}

.score-display {
  font-size: 24px;
  font-weight: 700;
  color: #1890ff;
}

/* ===== 已保存笔记区域 ===== */
.saved-notes-section {
  margin-top: 4px;
}

.notes-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  cursor: pointer;
  border-radius: 8px;
  transition: background 0.2s;
  user-select: none;
}

.notes-header:hover {
  background: var(--bg-block-light);
}

.notes-title {
  font-size: 13px;
  font-weight: 600;
  color: #333;
  display: flex;
  align-items: center;
}

.notes-body {
  padding: 0 8px 8px;
}

.notes-empty {
  text-align: center;
  padding: 16px 0;
  color: #bbb;
}

.notes-empty p {
  margin: 0;
  font-size: 13px;
}

.notes-empty-hint {
  font-size: 12px;
  margin-top: 4px !important;
}

.note-item {
  padding: 8px 10px;
  margin-bottom: 6px;
  background: var(--bg-block-light);
  border-radius: 8px;
  border: 1px solid var(--border-color);
  transition: box-shadow 0.2s;
}

.note-item:hover {
  box-shadow: 0 2px 8px rgba(74, 144, 217, 0.12);
}

.note-item-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
  gap: 6px;
}

.note-item-title {
  font-size: 12px;
  font-weight: 600;
  color: #1a1a1a;
  flex: 1;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.note-item-title:hover {
  color: var(--primary-color);
}

.note-item-content {
  font-size: 12px;
  color: #555;
  line-height: 1.5;
  cursor: pointer;
  /* 默认折叠：显示前50字（约2行）*/
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.note-item-content.expanded {
  /* 展开：显示全文 */
  display: block;
  -webkit-line-clamp: unset;
  overflow: visible;
}

.note-item-question {
  margin-top: 4px;
  font-size: 11px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
