<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import {
  createTool,
  deleteTool,
  listToolCategories,
  listTools,
  updateTool
} from '@/api/console'
import type { ToolItem, ToolPayload } from '@/api/console'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'
import PageHead from '@/components/ui/PageHead.vue'
import PageTabs from '@/components/ui/PageTabs.vue'
import AppIcon from '@/components/ui/AppIcon.vue'

const auth = useAuthStore()
const ui = useUiStore()

const tabs = [
  { label: '工具分类目录', to: '/hub/tools' },
  { label: 'MCP Server 注册', to: '/hub/servers' },
  { label: '接口调试', to: '/hub/debug' }
]

const filters = ref<string[]>(['全部'])
const active = ref('全部')
const keyword = ref('')

const tools = ref<ToolItem[]>([])
const loading = ref(false)
const loadError = ref('')
const readonly = computed(() => auth.role === 'ops')

function errorText(error: unknown) {
  return error instanceof Error ? error.message : '请求失败'
}

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    const page = await listTools({
      page: 1,
      size: 100,
      category: active.value === '全部' ? undefined : active.value,
      keyword: keyword.value.trim() || undefined
    })
    tools.value = page.records
  } catch (error) {
    loadError.value = errorText(error)
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  try {
    const cats = await listToolCategories()
    filters.value = ['全部', ...cats]
  } catch {
    filters.value = ['全部']
  }
}

let timer: number | undefined
watch([active, keyword], () => {
  window.clearTimeout(timer)
  timer = window.setTimeout(load, 250)
})
onBeforeUnmount(() => window.clearTimeout(timer))

onMounted(() => {
  loadCategories()
  load()
})

/* ---------- 新建 / 编辑 ---------- */
const formOpen = ref(false)
const saving = ref(false)
const editingCode = ref('')
const form = ref<ToolPayload>(emptyForm())

function emptyForm(): ToolPayload {
  return {
    name: '',
    code: '',
    cat: '交易类',
    proto: 'RPC',
    status: '在线',
    timeout: '800ms',
    latency: '—',
    risk: false
  }
}

function openCreate() {
  editingCode.value = ''
  form.value = emptyForm()
  formOpen.value = true
}

function openEdit(tool: ToolItem) {
  editingCode.value = tool.code
  form.value = {
    name: tool.name,
    code: tool.code,
    cat: tool.cat,
    proto: tool.proto,
    status: tool.status,
    timeout: tool.timeout,
    latency: tool.latency,
    risk: tool.risk
  }
  formOpen.value = true
}

function closeForm() {
  if (saving.value) return
  formOpen.value = false
}

async function save() {
  if (!form.value.name.trim()) {
    ui.toast('请填写工具名称', 'err')
    return
  }
  if (!form.value.code.trim()) {
    ui.toast('请填写工具 Code', 'err')
    return
  }
  saving.value = true
  try {
    if (editingCode.value) {
      await updateTool(editingCode.value, form.value)
      ui.toast('工具已更新', 'ok')
    } else {
      await createTool(form.value)
      ui.toast('工具已创建', 'ok')
    }
    formOpen.value = false
    await Promise.all([load(), loadCategories()])
  } catch (error) {
    ui.toast(errorText(error), 'err')
  } finally {
    saving.value = false
  }
}

async function remove(tool: ToolItem) {
  if (!window.confirm(`确定删除工具「${tool.name}」？`)) return
  try {
    await deleteTool(tool.code)
    ui.toast('工具已删除', 'ok')
    await load()
  } catch (error) {
    ui.toast(errorText(error), 'err')
  }
}

function debug(tool: ToolItem) {
  ui.toast(readonly.value ? '只读角色不可调试工具' : `已打开 ${tool.name} 的调试面板`, readonly.value ? 'err' : '')
}
</script>

<template>
  <PageHead
    eyebrow="HUB / 工具与插件广场"
    title="工具分类目录"
    sub="统一治理 RPC 与 MCP 工具：超时、降级话术与风险等级由平台统一配置。"
  >
    <template #actions>
      <button v-if="!readonly" class="btn btn-primary" type="button" @click="openCreate">
        <AppIcon name="plus" :size="14" />注册工具
      </button>
      <span v-else class="tag tag-neutral">只读视角</span>
    </template>
  </PageHead>

  <PageTabs :items="tabs" />

  <div v-if="readonly" class="notice" style="margin-bottom: 14px">
    <AppIcon name="shield" :size="15" />
    <span>运维/主管角色可查看工具目录与健康状态，新增、编辑与调试入口仅管理员可用。</span>
  </div>

  <section class="panel">
    <header class="panel-head" style="flex-wrap: wrap; gap: 12px">
      <div class="chips">
        <button
          v-for="item in filters"
          :key="item"
          class="chip"
          :class="{ 'is-on': active === item }"
          type="button"
          @click="active = item"
        >
          {{ item }}
        </button>
      </div>
      <div class="searchbox" style="margin-left: auto; min-width: 220px">
        <AppIcon name="search" :size="13" />
        <input
          v-model="keyword"
          type="text"
          placeholder="搜索工具名称 / code"
          style="border: 0; background: transparent; outline: none; width: 100%; font-size: 11.5px"
          aria-label="搜索工具"
        />
      </div>
      <span class="panel-meta">{{ tools.length }} TOOLS</span>
    </header>
    <div class="panel-body">
      <div class="tool-grid">
        <article v-for="tool in tools" :key="tool.code" class="tool-card" :class="{ 'is-risk': tool.risk }">
          <div class="tc-top">
            <span class="tc-ico"><AppIcon :name="tool.risk ? 'alert' : 'zap'" :size="14" /></span>
            <div style="flex: 1; min-width: 0">
              <div class="tc-name">{{ tool.name }}</div>
              <div class="tc-code">{{ tool.code }}</div>
            </div>
            <span class="tag" :class="tool.status === '在线' ? 'tag-ok' : 'tag-warn'">{{ tool.status }}</span>
          </div>
          <div class="chips" style="margin-top: 10px">
            <span class="tag tag-neutral">{{ tool.cat }}</span>
            <span class="tag tag-info">{{ tool.proto }}</span>
            <span v-if="tool.risk" class="tag tag-danger">高危 · 写操作</span>
          </div>
          <div class="tc-meta">
            <div>超时<b>{{ tool.timeout }}</b></div>
            <div>P95 时延<b>{{ tool.latency }}</b></div>
            <div style="margin-left: auto; align-self: flex-end; display: flex; gap: 6px">
              <button v-if="!readonly" class="btn btn-sm btn-quiet" type="button" @click="openEdit(tool)">编辑</button>
              <button v-if="!readonly" class="btn btn-sm btn-quiet" type="button" @click="remove(tool)">删除</button>
              <button class="btn btn-sm btn-quiet" type="button" @click="debug(tool)">
                {{ readonly ? '查看' : '调试' }}
              </button>
            </div>
          </div>
        </article>
      </div>
      <div v-if="loading" style="padding: 40px; text-align: center; color: var(--faint)">加载中…</div>
      <div v-else-if="loadError" style="padding: 40px; text-align: center; color: var(--danger)">{{ loadError }}</div>
      <div v-else-if="!tools.length" style="padding: 40px; text-align: center; color: var(--faint)">
        没有符合条件的工具，试试调整筛选或注册一个。
      </div>
    </div>
  </section>

  <Teleport to="body">
    <div v-if="formOpen" class="modal-backdrop" style="z-index: 120" @click.self="closeForm">
      <div class="modal" style="width: min(580px, 100%)" role="dialog" aria-modal="true">
        <div class="modal-head">
          <div>
            <h3>{{ editingCode ? '编辑工具' : '注册工具' }}</h3>
            <div class="mh-sub">工具 Code 全局唯一，风险等级会影响 Agent 挂载时的二次确认</div>
          </div>
          <button class="btn btn-sm x" type="button" @click="closeForm">关闭</button>
        </div>
        <div class="modal-body" style="padding: 18px 20px">
          <div class="grid g2">
            <div class="field">
              <label class="field-label" for="tool-name">名称<span class="req">*</span></label>
              <input id="tool-name" v-model="form.name" class="input" type="text" placeholder="查交易明细" />
            </div>
            <div class="field">
              <label class="field-label" for="tool-code">Code<span class="req">*</span></label>
              <input
                id="tool-code"
                v-model="form.code"
                class="input"
                type="text"
                placeholder="queryTransaction"
                :disabled="Boolean(editingCode)"
              />
            </div>
            <div class="field">
              <label class="field-label" for="tool-cat">分类</label>
              <input id="tool-cat" v-model="form.cat" class="input" type="text" list="tool-cat-options" placeholder="交易类" />
              <datalist id="tool-cat-options">
                <option v-for="item in filters.filter((cat) => cat !== '全部')" :key="item" :value="item"></option>
              </datalist>
            </div>
            <div class="field">
              <label class="field-label" for="tool-proto">协议</label>
              <select id="tool-proto" v-model="form.proto" class="select">
                <option value="RPC">RPC</option>
                <option value="MCP">MCP</option>
                <option value="HTTP">HTTP</option>
              </select>
            </div>
            <div class="field">
              <label class="field-label" for="tool-status">状态</label>
              <select id="tool-status" v-model="form.status" class="select">
                <option value="在线">在线</option>
                <option value="降级">降级</option>
              </select>
            </div>
            <div class="field">
              <label class="field-label" for="tool-timeout">超时配置</label>
              <input id="tool-timeout" v-model="form.timeout" class="input" type="text" placeholder="800ms" />
            </div>
            <div class="field">
              <label class="field-label" for="tool-latency">P95 时延</label>
              <input id="tool-latency" v-model="form.latency" class="input" type="text" placeholder="86ms" />
            </div>
            <div class="field">
              <label class="field-label" for="tool-risk">风险等级</label>
              <select id="tool-risk" v-model="form.risk" class="select">
                <option :value="false">只读 · 低风险</option>
                <option :value="true">高危 · 写操作</option>
              </select>
            </div>
          </div>
        </div>
        <div class="modal-foot">
          <span class="mf-spacer"></span>
          <button class="btn" type="button" @click="closeForm">取消</button>
          <button class="btn btn-primary" type="button" :disabled="saving" @click="save">
            {{ saving ? '保存中…' : '保存' }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>
