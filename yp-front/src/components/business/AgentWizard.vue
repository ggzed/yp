<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import {
  GUARDS,
  HIGH_RISK_TOOL_IDS,
  WIZARD_STEPS,
  WIZARD_TOOL_CATS,
  createWizardForm
} from '@/data/console'
import type { WizardForm, WizardTool } from '@/data/console'
import { createAgent, publishAgent, updateAgent } from '@/api/agents'
import type { AgentDetail } from '@/api/agents'
import { useUiStore } from '@/stores/ui'
import AppIcon from '@/components/ui/AppIcon.vue'
import DangerConfirm from '@/components/business/DangerConfirm.vue'

const props = defineProps<{
  open: boolean
  agent?: AgentDetail | null
}>()

const emit = defineEmits<{ close: []; published: [] }>()

const ui = useUiStore()
const step = ref(0)
const maxStep = ref(0)
const form = ref<WizardForm>(createWizardForm())
const error = ref('')
const success = ref(false)
const dataId = ref('')
const pendingTool = ref<WizardTool | null>(null)
const confirmOpen = ref(false)
const publishing = ref(false)

const editing = computed(() => Boolean(props.agent))
const title = computed(() => (editing.value ? '编辑智能体' : '新建智能体'))

const selectedTools = computed(() =>
  WIZARD_TOOL_CATS.flatMap((cat) => cat.items).filter((tool) => form.value.tools.includes(tool.id))
)
const hasRiskTool = computed(() => form.value.tools.some((id) => HIGH_RISK_TOOL_IDS.includes(id)))
const toolNames = computed(() => selectedTools.value.map((tool) => tool.name).join('、') || '未挂载')
const guardNames = computed(() => {
  const picked = GUARDS.filter((guard) => form.value.guards.includes(guard.id)).map((guard) => guard.name)
  return picked.join('、') || '未启用'
})

const nacos = computed(() => {
  const tools = form.value.tools.length ? form.value.tools.join(', ') : '[]'
  const guards = form.value.guards.length ? form.value.guards.join(', ') : '[]'
  return `# 已写入 Nacos · risk-agent / prod
agent:
  code: ${form.value.code || 'auto_generated'}
  model: ${form.value.model}
  kb: ${form.value.kb || 'none'}
  top_k: ${form.value.topK}
  tools: [${tools}]
  guards: [${guards}]
  quota: ${form.value.quota.toLocaleString()} tokens/日
  owner: linlan@platform`
})

function init() {
  step.value = 0
  maxStep.value = 0
  error.value = ''
  success.value = false
  pendingTool.value = null
  confirmOpen.value = false
  publishing.value = false

  const base = createWizardForm()
  const detail = props.agent
  form.value = detail
    ? createWizardForm({
        name: detail.name,
        code: detail.code,
        type: (detail.type as WizardForm['type']) || base.type,
        desc: detail.desc ?? base.desc,
        dept: detail.dept ?? base.dept,
        model: detail.model ?? base.model,
        temp: detail.temp ?? base.temp,
        topP: detail.topP ?? base.topP,
        prompt: detail.prompt ?? base.prompt,
        kb: detail.kb ?? base.kb,
        topK: detail.topK ?? base.topK,
        threshold: detail.threshold ?? base.threshold,
        hybrid: detail.hybrid ?? base.hybrid,
        memory: detail.memory ?? base.memory,
        rounds: detail.rounds ?? base.rounds,
        tools: detail.tools ?? base.tools,
        guards: detail.guards ?? base.guards,
        quota: detail.quota ?? base.quota,
        note: detail.note ?? base.note
      })
    : base
}

watch(
  () => props.open,
  (open) => {
    if (open) init()
  },
  { immediate: true }
)

function close() {
  if (confirmOpen.value) return
  emit('close')
}

function go(index: number) {
  if (index <= maxStep.value) {
    step.value = index
    error.value = ''
  }
}

function back() {
  if (step.value > 0) {
    step.value -= 1
    error.value = ''
  }
}

function next() {
  if (step.value === 0) {
    if (!form.value.name.trim()) {
      error.value = '请填写 Agent 名称'
      return
    }
    if (form.value.code && !/^[a-z][a-z0-9_]{2,}$/.test(form.value.code)) {
      error.value = 'Code 需以小写字母开头，仅含小写字母、数字与下划线'
      return
    }
  }
  error.value = ''

  if (step.value === WIZARD_STEPS.length - 1) {
    publish()
    return
  }

  step.value += 1
  maxStep.value = Math.max(maxStep.value, step.value)
}

function isChecked(id: string) {
  return form.value.tools.includes(id)
}

function toggleTool(tool: WizardTool) {
  if (isChecked(tool.id)) {
    form.value.tools = form.value.tools.filter((id) => id !== tool.id)
    return
  }
  if (tool.risk) {
    pendingTool.value = tool
    confirmOpen.value = true
    return
  }
  form.value.tools = [...form.value.tools, tool.id]
}

function confirmRisk() {
  if (pendingTool.value) {
    form.value.tools = [...form.value.tools, pendingTool.value.id]
    ui.toast('已加入高危工具，发布前必须启用「高危阻断」护栏', '')
  }
  pendingTool.value = null
  confirmOpen.value = false
}

function cancelRisk() {
  pendingTool.value = null
  confirmOpen.value = false
}

function toggleGuard(id: string) {
  form.value.guards = form.value.guards.includes(id)
    ? form.value.guards.filter((item) => item !== id)
    : [...form.value.guards, id]
}

async function publish() {
  if (publishing.value) return
  if (hasRiskTool.value && !form.value.guards.includes('block')) {
    step.value = WIZARD_STEPS.length - 1
    maxStep.value = WIZARD_STEPS.length - 1
    error.value = '已挂载高危工具，必须启用「高危阻断」护栏后才能发布'
    ui.toast('发布被拦截：高危工具缺少阻断护栏', 'err')
    return
  }

  const code = form.value.code.trim() || `agent_${Math.random().toString(36).slice(2, 8)}`
  form.value.code = code

  const payload: AgentDetail = {
    name: form.value.name.trim(),
    code,
    type: form.value.type,
    desc: form.value.desc,
    dept: form.value.dept,
    model: form.value.model,
    temp: form.value.temp,
    topP: form.value.topP,
    prompt: form.value.prompt,
    kb: form.value.kb,
    topK: form.value.topK,
    threshold: form.value.threshold,
    hybrid: form.value.hybrid,
    memory: form.value.memory,
    rounds: form.value.rounds,
    tools: form.value.tools,
    guards: form.value.guards,
    quota: form.value.quota,
    note: form.value.note
  }

  publishing.value = true
  error.value = ''
  try {
    if (editing.value && props.agent) {
      await updateAgent(props.agent.code, payload)
    } else {
      await createAgent(payload)
    }
    const result = await publishAgent(code)
    dataId.value = result.dataId
    success.value = true
    emit('published')
    ui.toast(editing.value ? '智能体配置已更新并生效' : '智能体已发布并写入 Nacos', 'ok')
  } catch (err) {
    error.value = err instanceof Error ? err.message : '发布失败，请稍后重试'
    ui.toast(error.value, 'err')
  } finally {
    publishing.value = false
  }
}

function onKey(event: KeyboardEvent) {
  if (event.key === 'Escape' && props.open && !confirmOpen.value) close()
}

onMounted(() => window.addEventListener('keydown', onKey))
onBeforeUnmount(() => window.removeEventListener('keydown', onKey))
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="modal-backdrop" @click.self="close">
      <div class="modal" role="dialog" aria-modal="true" :aria-label="title">
        <div class="modal-head">
          <AppIcon name="nodes" :size="18" />
          <div>
            <h3>{{ title }}</h3>
            <div class="mh-sub">
              {{ editing ? '修改配置后发布将即时生效' : '5 步完成配置 · 发布后写入 Nacos 并生成可调用端点' }}
            </div>
          </div>
          <button class="icon-btn x" type="button" aria-label="关闭" @click="close">
            <AppIcon name="x" :size="15" />
          </button>
        </div>

        <div class="modal-body">
          <div v-if="success" class="success-view">
            <div class="success-ico"><AppIcon name="check" :size="26" /></div>
            <h4>{{ editing ? '配置已更新' : '发布成功' }}</h4>
            <p>
              「{{ form.name }}」已写入 Nacos 配置中心并生成端点。<br />
              dataId = <span class="mono">{{ dataId }}</span> · 预计 30 秒内完成灰度生效。
            </p>
            <div class="code-box" style="text-align: left">{{ nacos }}</div>
            <p class="hint" style="margin-top: 12px">
              已挂载 {{ form.tools.length }} 个工具、{{ form.guards.length }} 项护栏；当日配额
              {{ form.quota.toLocaleString() }} tokens，超出后自动限流。
            </p>
          </div>

          <div v-else class="wizard">
            <div class="wizard-rail" aria-label="配置步骤">
              <button
                v-for="(label, index) in WIZARD_STEPS"
                :key="label"
                type="button"
                class="wstep"
                :class="{
                  'is-active': step === index,
                  'is-done': index < step && index <= maxStep
                }"
                :disabled="index > maxStep"
                @click="go(index)"
              >
                <span class="ws-n">
                  <AppIcon v-if="index < step" name="check" :size="11" />
                  <template v-else>{{ index + 1 }}</template>
                </span>
                <span class="ws-t">{{ label }}</span>
              </button>
            </div>

            <div class="wizard-main">
              <div v-if="error" class="error" style="margin-bottom: 12px">{{ error }}</div>

              <template v-if="step === 0">
                <h4>基础信息</h4>
                <p class="wm-sub">名称用于列表展示；Code 是运行时的唯一标识，发布后不可随意更改。</p>
                <div class="split-2">
                  <div class="field">
                    <label class="field-label" for="agent-name">Agent 名称<span class="req">*</span></label>
                    <input id="agent-name" v-model="form.name" class="input" placeholder="例如：反欺诈话术审查" />
                  </div>
                  <div class="field">
                    <label class="field-label" for="agent-code">Agent Code</label>
                    <input id="agent-code" v-model="form.code" class="mono input" placeholder="留空自动生成，如 risk_qa_v4" :disabled="editing" />
                  </div>
                </div>

                <div class="field">
                  <span class="field-label">Agent 类型</span>
                  <div class="radio-cards">
                    <label class="radio-card" :class="{ 'is-on': form.type === '问答型' }">
                      <input v-model="form.type" type="radio" value="问答型" name="agent-type" />
                      <span>
                        <span class="rc-t">问答型</span>
                        <span class="rc-d">单轮 / 多轮问答，基于 RAG 给出可解释回答</span>
                      </span>
                    </label>
                    <label class="radio-card" :class="{ 'is-on': form.type === '长链路型' }">
                      <input v-model="form.type" type="radio" value="长链路型" name="agent-type" />
                      <span>
                        <span class="rc-t">长链路型</span>
                        <span class="rc-d">自动拆解任务，多工具调用与聚合计算</span>
                      </span>
                    </label>
                  </div>
                </div>

                <div class="field">
                  <label class="field-label" for="agent-desc">用途说明</label>
                  <textarea id="agent-desc" v-model="form.desc" class="textarea" placeholder="一句话说明该 Agent 解决什么问题、面向哪些部门"></textarea>
                </div>

                <div class="field" style="margin-bottom: 0">
                  <label class="field-label" for="agent-dept">归属部门</label>
                  <select id="agent-dept" v-model="form.dept" class="select">
                    <option>零售业务部</option>
                    <option>信用卡中心</option>
                    <option>小微金融</option>
                    <option>公司金融部</option>
                    <option>风控运营中心</option>
                  </select>
                  <span class="hint">Token 账单将按归属部门归集，用于月度对账。</span>
                </div>
              </template>

              <template v-else-if="step === 1">
                <h4>模型与提示词</h4>
                <p class="wm-sub">模型决定时延与成本；提示词须包含「无依据不推断」的约束。</p>

                <div class="field">
                  <label class="field-label" for="agent-model">推理模型</label>
                  <select id="agent-model" v-model="form.model" class="select">
                    <option>DeepSeek-V3</option>
                    <option>Qwen2.5-72B</option>
                    <option>豆包Pro-32K</option>
                  </select>
                </div>

                <div class="split-2">
                  <div class="field">
                    <label class="field-label" for="agent-temp">
                      温度 <span class="inline-val">{{ form.temp.toFixed(2) }}</span>
                    </label>
                    <input id="agent-temp" v-model.number="form.temp" class="range" type="range" min="0" max="1" step="0.05" />
                    <span class="hint">风控场景建议 0.1 – 0.4，越低越稳定。</span>
                  </div>
                  <div class="field">
                    <label class="field-label" for="agent-topp">
                      Top-P <span class="inline-val">{{ form.topP.toFixed(2) }}</span>
                    </label>
                    <input id="agent-topp" v-model.number="form.topP" class="range" type="range" min="0.1" max="1" step="0.05" />
                    <span class="hint">控制采样多样性，默认 0.90。</span>
                  </div>
                </div>

                <div class="field" style="margin-bottom: 0">
                  <label class="field-label" for="agent-prompt">系统提示词</label>
                  <textarea id="agent-prompt" v-model="form.prompt" class="textarea" style="min-height: 148px"></textarea>
                  <span class="hint">支持占位符 {context} 与 {user_query}，由编排引擎注入检索结果与用户问题。</span>
                </div>
              </template>

              <template v-else-if="step === 2">
                <h4>知识与记忆</h4>
                <p class="wm-sub">挂载知识库后启用混合检索；阈值过低会引入噪声，过高会漏召回。</p>

                <div class="split-2">
                  <div class="field">
                    <label class="field-label" for="agent-kb">知识库</label>
                    <select id="agent-kb" v-model="form.kb" class="select">
                      <option value="risk_rules_v3">risk_rules_v3 · 风控规则库</option>
                      <option value="collection_rules">collection_rules · 催收合规库</option>
                      <option value="none">不挂载知识库</option>
                    </select>
                  </div>
                  <div class="field">
                    <label class="field-label" for="agent-topk">Top-K</label>
                    <input id="agent-topk" v-model.number="form.topK" class="input num" type="number" min="1" max="20" />
                    <span class="hint">单次注入的片段数量，建议 3 – 8。</span>
                  </div>
                </div>

                <div class="field">
                  <label class="field-label" for="agent-threshold">
                    召回阈值 <span class="inline-val">{{ form.threshold.toFixed(2) }}</span>
                  </label>
                  <input id="agent-threshold" v-model.number="form.threshold" class="range" type="range" min="0.3" max="0.95" step="0.01" />
                  <span class="hint">低于阈值的片段不会进入上下文；可在「召回测试」中验证。</span>
                </div>

                <div class="switch-row">
                  <div>
                    <div class="sw-t">混合检索 BM25 + KNN</div>
                    <div class="sw-d">关键词与向量双通道召回，提升术语命中率</div>
                  </div>
                  <input v-model="form.hybrid" type="checkbox" style="accent-color: var(--brand); width: 18px; height: 18px" />
                </div>
                <div class="switch-row">
                  <div>
                    <div class="sw-t">多轮记忆</div>
                    <div class="sw-d">保留最近 {{ form.rounds }} 轮对话上下文</div>
                  </div>
                  <input v-model="form.memory" type="checkbox" style="accent-color: var(--brand); width: 18px; height: 18px" />
                </div>
                <div v-if="form.memory" class="field" style="margin-top: 14px; margin-bottom: 0">
                  <label class="field-label" for="agent-rounds">记忆轮数 <span class="inline-val">{{ form.rounds }}</span></label>
                  <input id="agent-rounds" v-model.number="form.rounds" class="range" type="range" min="1" max="20" step="1" />
                </div>
              </template>

              <template v-else-if="step === 3">
                <h4>工具挂载</h4>
                <p class="wm-sub">工具来自「工具与插件广场」已注册的 MCP / RPC 服务；写操作工具需要二次确认。</p>

                <div class="notice warn" style="margin-bottom: 14px">
                  <AppIcon name="alert" :size="15" />
                  <span>挂载高危工具（冻结额度 / 大额扣款）会触发二次确认，且发布前必须启用「高危阻断」护栏。</span>
                </div>

                <template v-for="cat in WIZARD_TOOL_CATS" :key="cat.category">
                  <div class="tool-cat">{{ cat.category }}</div>
                  <label
                    v-for="tool in cat.items"
                    :key="tool.id"
                    class="check-row"
                    :class="{ 'is-risk': tool.risk, 'is-checked': isChecked(tool.id) }"
                  >
                    <input
                      type="checkbox"
                      :checked="isChecked(tool.id)"
                      @change="toggleTool(tool)"
                    />
                    <span class="cr-body">
                      <span class="cr-t">
                        {{ tool.name }}
                        <span v-if="tool.risk" class="tag tag-danger">高危</span>
                      </span>
                      <span class="cr-d">{{ tool.desc }}</span>
                    </span>
                  </label>
                </template>

                <p class="hint" style="margin-top: 12px">
                  已选 {{ form.tools.length }} 个工具：{{ toolNames }}
                </p>
              </template>

              <template v-else>
                <h4>安全与治理</h4>
                <p class="wm-sub">护栏在输入与输出两侧生效，所有工具调用都会写入合规审计日志。</p>

                <div class="field">
                  <span class="field-label">护栏策略</span>
                  <label
                    v-for="guard in GUARDS"
                    :key="guard.id"
                    class="check-row"
                    :class="{
                      'is-checked': form.guards.includes(guard.id),
                      'is-risk': hasRiskTool && guard.id === 'block' && !form.guards.includes('block')
                    }"
                  >
                    <input
                      type="checkbox"
                      :checked="form.guards.includes(guard.id)"
                      @change="toggleGuard(guard.id)"
                    />
                    <span class="cr-body">
                      <span class="cr-t">
                        {{ guard.name }}
                        <span v-if="hasRiskTool && guard.id === 'block'" class="tag tag-danger">必选</span>
                      </span>
                      <span class="cr-d">{{ guard.desc }}</span>
                    </span>
                  </label>
                </div>

                <div class="field">
                  <label class="field-label" for="agent-quota">
                    日 Token 配额 <span class="inline-val">{{ form.quota.toLocaleString() }}</span>
                  </label>
                  <input id="agent-quota" v-model.number="form.quota" class="range" type="range" min="10000" max="200000" step="5000" />
                  <span class="hint">超出配额后自动限流并通知负责人，防止异常刷量。</span>
                </div>

                <div class="field">
                  <label class="field-label" for="agent-note">发布备注</label>
                  <textarea id="agent-note" v-model="form.note" class="textarea" style="min-height: 72px" placeholder="例如：本次调整了召回阈值与护栏策略"></textarea>
                </div>

                <div class="summary-box">
                  <div class="summary-row"><span class="sk">Agent</span><span class="sv">{{ form.name || '未填写' }}</span></div>
                  <div class="summary-row"><span class="sk">模型</span><span class="sv">{{ form.model }}</span></div>
                  <div class="summary-row"><span class="sk">知识库</span><span class="sv">{{ form.kb }} · top_k {{ form.topK }} · 阈值 {{ form.threshold.toFixed(2) }}</span></div>
                  <div class="summary-row"><span class="sk">工具</span><span class="sv">{{ toolNames }}</span></div>
                  <div class="summary-row"><span class="sk">护栏</span><span class="sv">{{ guardNames }}</span></div>
                </div>
              </template>
            </div>
          </div>
        </div>

        <div class="modal-foot">
          <span class="hint mono" v-if="!success">STEP {{ step + 1 }} / {{ WIZARD_STEPS.length }}</span>
          <span class="mf-spacer"></span>
          <template v-if="success">
            <button class="btn btn-primary" type="button" @click="emit('close')">完成</button>
          </template>
          <template v-else>
            <button class="btn" type="button" @click="emit('close')">取消</button>
            <button class="btn" type="button" :disabled="step === 0" @click="back">上一步</button>
            <button class="btn btn-primary" type="button" :disabled="publishing" @click="next">
              {{ publishing ? '发布中…' : step === WIZARD_STEPS.length - 1 ? '发布' : '下一步' }}
              <AppIcon v-if="step < WIZARD_STEPS.length - 1" name="arrow" :size="13" />
              <AppIcon v-else name="check" :size="13" />
            </button>
          </template>
        </div>
      </div>

      <DangerConfirm
        :open="confirmOpen"
        :tool-name="pendingTool?.name ?? ''"
        :tool-code="pendingTool?.desc.split(' · ')[0] ?? ''"
        @cancel="cancelRisk"
        @confirm="confirmRisk"
      />
    </div>
  </Teleport>
</template>
