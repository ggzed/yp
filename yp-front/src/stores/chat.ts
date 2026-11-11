import { ref } from 'vue'
import { defineStore } from 'pinia'
import { CHAT_SEEDS } from '@/data/console'
import { chatWithWorkflow } from '@/api/workflow'

export type ChatMode = 'rule' | 'data' | 'workflow'

export interface ChatMessage {
  id: number
  role: 'user' | 'ai'
  content: string
  time: string
  typing?: boolean
}

let seed = 0

function createSessionId(): string {
  return `wf-${Date.now()}`
}

function now(): string {
  const date = new Date()
  return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

function seedMessages(mode: ChatMode): ChatMessage[] {
  // 编排引擎对话没有演示数据，每次进入都是空白会话
  if (mode === 'workflow') return []
  return CHAT_SEEDS[mode].map((item) => ({
    id: ++seed,
    role: item.role,
    content: item.content,
    time: item.time
  }))
}

/** 两个消费者对话页互不干扰，各自维护消息与流式输出状态。 */
export const useChatStore = defineStore('chat', () => {
  const conversations = ref<Record<ChatMode, ChatMessage[]>>({
    rule: seedMessages('rule'),
    data: seedMessages('data'),
    workflow: seedMessages('workflow')
  })
  const streaming = ref<Record<ChatMode, boolean>>({ rule: false, data: false, workflow: false })
  const revision = ref(0)
  /** 编排引擎的会话标识，由前端生成后随每次请求带给后端 */
  const workflowSessionId = ref(createSessionId())

  function reset(mode: ChatMode) {
    revision.value++
    conversations.value[mode] = seedMessages(mode)
    streaming.value[mode] = false
    if (mode === 'workflow') workflowSessionId.value = createSessionId()
  }

  function sleep(ms: number) {
    return new Promise<void>((resolve) => window.setTimeout(resolve, ms))
  }

  async function send(mode: ChatMode, text: string) {
    const content = text.trim()
    if (!content || streaming.value[mode]) return

    const run = revision.value
    const list = conversations.value[mode]
    list.push({ id: ++seed, role: 'user', content, time: now() })

    const reply: ChatMessage = { id: ++seed, role: 'ai', content: '', time: now(), typing: true }
    list.push(reply)
    streaming.value[mode] = true

    // 编排引擎：走真实接口 GET /workflow/chat/chat，失败时把错误信息作为回答展示
    if (mode === 'workflow') {
      try {
        const data = await chatWithWorkflow({
          agentId: 'risk-agent',
          sessionId: workflowSessionId.value,
          query: content
        })
        if (run !== revision.value) return
        reply.content = data.content || '（模型未返回内容）'
      } catch (err) {
        if (run !== revision.value) return
        reply.content = err instanceof Error ? err.message : '对话请求失败'
      } finally {
        if (run === revision.value) {
          reply.typing = false
          reply.time = now()
          streaming.value[mode] = false
        }
      }
      return
    }

    const answer =
      mode === 'rule'
        ? '已检索规则库并找到 2 条相关条款。\n\n依据《大额交易拦截管理办法》4.2 条与《个人信贷风控规则手册》3.6 节：该场景需触发双人复核，并在 T+0 内冻结对应额度直至复核完成。\n\n右侧已同步更新本次命中的参考文档与 BM25 / KNN 得分，可逐条核对。'
        : '收到，我已将该问题拆解为 5 步执行计划。\n\n正在调用交易查询接口取数 → 按维度聚合 → 召回统计口径规则校验 → 汇总生成结论。右侧执行时间线会实时反映每一步的耗时与状态。\n\n任务完成：本次共检索 1,284 条记录，结果已渲染在右侧图表中。'

    await sleep(620)
    reply.typing = false

    for (let index = 3; index <= answer.length; index += 3) {
      if (run !== revision.value) return
      reply.content = answer.slice(0, index)
      await sleep(16)
    }
    reply.content = answer
    reply.time = now()
    streaming.value[mode] = false
  }

  return { conversations, streaming, workflowSessionId, reset, send }
})
