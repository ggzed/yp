<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { REFS } from '@/data/console'
import { useAuthStore } from '@/stores/auth'
import { useChatStore } from '@/stores/chat'
import type { ChatMode } from '@/stores/chat'
import { useUiStore } from '@/stores/ui'
import PageHead from '@/components/ui/PageHead.vue'
import PageTabs from '@/components/ui/PageTabs.vue'
import AppIcon from '@/components/ui/AppIcon.vue'
import RagRefPanel from '@/components/business/RagRefPanel.vue'
import ExecTimeline from '@/components/business/ExecTimeline.vue'

const route = useRoute()
const auth = useAuthStore()
const chat = useChatStore()
const ui = useUiStore()

const tabs = [
  { label: '我的 Agent', to: '/apps' },
  { label: '风控规则助手', to: '/apps/rule-qa' },
  { label: '风控数据智算', to: '/apps/analytics' },
  { label: '工作流对话', to: '/apps/workflow-chat' }
]

const mode = computed<ChatMode>(() => {
  const meta = route.meta.mode
  return meta === 'data' || meta === 'workflow' ? meta : 'rule'
})
const appName = computed(() => {
  if (mode.value === 'rule') return '风控规则助手'
  if (mode.value === 'data') return '风控数据智算'
  return 'Agent 工作流对话'
})
const subtitle = computed(() => {
  if (mode.value === 'rule') return '解释型 Agent · 回答附带规则原文与检索得分'
  if (mode.value === 'data') return '长链路 Agent · 自动拆解任务并逐步展示执行过程'
  return '编排引擎 · 对话直达 yp-workflow-engine，按 Agent 配置执行工具编排'
})
const messages = computed(() => chat.conversations[mode.value])
const isStreaming = computed(() => chat.streaming[mode.value])
const logRef = ref<HTMLElement | null>(null)
const input = ref('')
const timelineRef = ref<InstanceType<typeof ExecTimeline> | null>(null)

const quickPrompts = computed(() => {
  if (mode.value === 'rule') {
    return ['大额转账需要谁审批？', '额度冻结后如何解冻？', '命中黑名单会怎样处理？']
  }
  if (mode.value === 'data') {
    return ['统计昨天刚控数量，并按部门分组', '分析本周高危客户变化', '生成昨日风控日报']
  }
  return ['帮我评估一笔大额转账的风险', '当前有哪些规则需要复核？', '生成一份今日风险简报']
})

const placeholder = computed(() => {
  if (mode.value === 'rule') return '询问风控规则，Enter 发送，Shift+Enter 换行…'
  if (mode.value === 'data') return '描述一个统计任务，例如：统计昨天刚控数量并按部门分组…'
  return '与编排 Agent 对话，Enter 发送，Shift+Enter 换行…'
})

function scrollToBottom() {
  nextTick(() => {
    if (logRef.value) logRef.value.scrollTop = logRef.value.scrollHeight
  })
}

watch(messages, scrollToBottom, { deep: true })
watch(mode, scrollToBottom, { immediate: true })

function submit(text = input.value) {
  const content = text.trim()
  if (!content || isStreaming.value) return
  input.value = ''
  void chat.send(mode.value, content)
  if (mode.value !== 'rule') timelineRef.value?.replay()
}

function ask(prompt: string) {
  submit(prompt)
}

function newSession() {
  chat.reset(mode.value)
  input.value = ''
  ui.toast('已开启新会话', 'ok')
}

function keyboard(event: KeyboardEvent) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    submit()
  }
}

function configHint() {
  if (auth.role !== 'admin') {
    ui.toast('业务员视角下 Agent 配置为只读', 'err')
    return
  }
  ui.toast('配置入口：智能体编排中心 → Agent 列表 → 配置', 'ok')
}
</script>

<template>
  <PageHead
    :eyebrow="`APPS / ${appName}`"
    :title="appName"
    :sub="subtitle"
  >
    <template #actions>
      <button class="btn" type="button" @click="configHint">
        <AppIcon name="sliders" :size="14" />Agent 配置
      </button>
      <button class="btn btn-primary" type="button" @click="newSession">
        <AppIcon name="plus" :size="14" />新建会话
      </button>
    </template>
  </PageHead>

  <PageTabs :items="tabs" />

  <div class="chat-layout">
    <section class="chat-panel" aria-label="对话区">
      <div class="chat-bar">
        <span class="dot dot-ok"></span>
        <span class="cb-title">{{ appName }}</span>
        <span class="tag tag-brand">{{ mode === 'rule' ? '问答型' : mode === 'data' ? '长链路型' : '编排型' }}</span>
        <span class="hint" style="margin: 0">{{ isStreaming ? '生成中…' : '在线' }}</span>
        <div class="cb-actions">
          <button class="btn btn-sm btn-quiet" type="button" @click="newSession">
            <AppIcon name="refresh" :size="12" />清空
          </button>
        </div>
      </div>

      <div ref="logRef" class="chat-log" aria-live="polite">
        <div v-for="message in messages" :key="message.id" class="msg" :class="`msg-${message.role}`">
          <div class="msg-av">{{ message.role === 'ai' ? 'AI' : auth.user.short }}</div>
          <div class="bubble">
            <div v-if="message.typing" class="typing"><i></i><i></i><i></i></div>
            <template v-else>
              <div>{{ message.content }}</div>
              <span class="b-time">{{ message.time }}</span>
            </template>
          </div>
        </div>
      </div>

      <div class="chips" style="padding: 0 14px 10px; background: var(--surface-2)">
        <button
          v-for="prompt in quickPrompts"
          :key="prompt"
          class="chip"
          type="button"
          @click="ask(prompt)"
        >
          {{ prompt }}
        </button>
      </div>

      <div class="chat-input">
        <div class="ci-row">
          <textarea
            v-model="input"
            :placeholder="placeholder"
            aria-label="对话输入"
            @keydown="keyboard"
          ></textarea>
          <button class="btn btn-primary" type="button" :disabled="isStreaming || !input.trim()" @click="submit()">
            <AppIcon name="send" :size="14" />{{ isStreaming ? '生成中' : '发送' }}
          </button>
        </div>
        <div class="ci-hint">
          <span>回答由 AI 生成，请以规则原文为准</span>
          <span>本次会话将计入部门 Token 账单</span>
          <span v-if="mode === 'rule'">已启用：敏感词过滤 / PII 脱敏</span>
          <span v-else-if="mode === 'data'">已启用：高危阻断 / 执行留痕</span>
          <span v-else>会话 ID：{{ chat.workflowSessionId }}</span>
        </div>
      </div>
    </section>

    <aside class="side-panel">
      <template v-if="mode === 'rule'">
        <div class="side-bar">
          <span class="side-title">参考来源</span>
          <span class="panel-meta" style="margin-left: auto">RAG / HYBRID</span>
          <button class="icon-btn" type="button" style="width: 26px; height: 26px" aria-label="刷新参考" @click="ui.toast('参考来源已刷新', 'ok')">
            <AppIcon name="refresh" :size="12" />
          </button>
        </div>
        <div class="side-body">
          <RagRefPanel :refs="REFS" />
        </div>
      </template>
      <template v-else>
        <ExecTimeline ref="timelineRef" />
      </template>
    </aside>
  </div>
</template>
