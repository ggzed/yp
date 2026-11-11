<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { createApp, deleteApp, listApps, updateApp } from '@/api/console'
import type { AppItem, AppPayload, AppStatus } from '@/api/console'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'
import PageHead from '@/components/ui/PageHead.vue'
import AppIcon from '@/components/ui/AppIcon.vue'

const auth = useAuthStore()
const ui = useUiStore()

const apps = ref<AppItem[]>([])
const loading = ref(false)
const loadError = ref('')
const canManage = computed(() => auth.role === 'admin')

const statusMeta: Record<AppStatus, { text: string; tag: string }> = {
  online: { text: '在线', tag: 'tag-ok' },
  gray: { text: '灰度', tag: 'tag-warn' },
  offline: { text: '已下线', tag: 'tag-neutral' }
}
const statusOptions: AppStatus[] = ['online', 'gray', 'offline']
const iconOptions = ['book', 'chart', 'chat', 'zap', 'nodes', 'server', 'db', 'shield']

function errorText(error: unknown) {
  return error instanceof Error ? error.message : '请求失败'
}

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    const page = await listApps({ page: 1, size: 100 })
    apps.value = page.records
  } catch (error) {
    loadError.value = errorText(error)
  } finally {
    loading.value = false
  }
}

onMounted(load)

/* ---------- 新建 / 编辑 ---------- */
const formOpen = ref(false)
const saving = ref(false)
const editingCode = ref('')
const form = ref<AppPayload>(emptyForm())

function emptyForm(): AppPayload {
  return {
    name: '',
    code: '',
    icon: 'chat',
    desc: '',
    calls: 0,
    latency: '—',
    satisfaction: '—',
    status: 'online',
    route: ''
  }
}

function openCreate() {
  editingCode.value = ''
  form.value = emptyForm()
  formOpen.value = true
}

function openEdit(app: AppItem) {
  editingCode.value = app.code
  form.value = {
    name: app.name,
    code: app.code,
    icon: app.icon || 'chat',
    desc: app.desc,
    calls: app.calls,
    latency: app.latency,
    satisfaction: app.satisfaction,
    status: app.status,
    route: app.route
  }
  formOpen.value = true
}

function closeForm() {
  if (saving.value) return
  formOpen.value = false
}

async function save() {
  if (!form.value.name.trim()) {
    ui.toast('请填写应用名称', 'err')
    return
  }
  if (!form.value.code.trim()) {
    ui.toast('请填写应用 Code', 'err')
    return
  }
  saving.value = true
  try {
    if (editingCode.value) {
      await updateApp(editingCode.value, form.value)
      ui.toast('应用已更新', 'ok')
    } else {
      await createApp(form.value)
      ui.toast('应用已创建', 'ok')
    }
    formOpen.value = false
    await load()
  } catch (error) {
    ui.toast(errorText(error), 'err')
  } finally {
    saving.value = false
  }
}

async function remove(app: AppItem) {
  if (!window.confirm(`确定删除应用「${app.name}」？`)) return
  try {
    await deleteApp(app.code)
    ui.toast('应用已删除', 'ok')
    await load()
  } catch (error) {
    ui.toast(errorText(error), 'err')
  }
}
</script>

<template>
  <PageHead
    eyebrow="APPS / 智能应用中心"
    title="我的 Agent"
    sub="仅展示当前角色已授权的 Agent。业务员可直接使用，管理员可前往编排中心维护配置。"
  >
    <template #actions>
      <button v-if="canManage" class="btn btn-primary" type="button" @click="openCreate">
        <AppIcon name="plus" :size="14" />新增 Agent 卡片
      </button>
      <RouterLink v-else to="/agents" class="btn btn-primary">
        <AppIcon name="nodes" :size="14" />前往编排中心
      </RouterLink>
    </template>
  </PageHead>

  <div class="notice" style="margin-bottom: 16px">
    <AppIcon name="shield" :size="15" />
    <span v-if="auth.role === 'sales'">业务员视角下，Agent 配置、工具挂载与知识库均为只读；如需调整请联系智能体平台组。</span>
    <span v-else>管理员视角可在此体验线上 Agent，也可在「智能体编排中心」修改提示词、工具与护栏后重新发布。</span>
  </div>

  <div class="grid g2">
    <article v-for="(app, index) in apps" :key="app.code" class="app-card">
      <div class="ac-top">
        <span class="ac-ico" :class="{ alt: index % 2 === 1 }"><AppIcon :name="app.icon || 'chat'" :size="19" /></span>
        <div style="flex: 1; min-width: 0">
          <h2 class="ac-name">{{ app.name }}</h2>
          <div class="ac-endpoint">{{ app.code }} · {{ statusMeta[app.status]?.text ?? app.status }}</div>
        </div>
        <span class="tag" :class="statusMeta[app.status]?.tag ?? 'tag-neutral'">
          {{ statusMeta[app.status]?.text ?? app.status }}
        </span>
      </div>
      <p class="ac-desc">{{ app.desc }}</p>
      <div class="ac-stats">
        <div>今日会话<b>{{ app.calls.toLocaleString() }}</b></div>
        <div>平均时延<b>{{ app.latency }}</b></div>
        <div>满意度<b>{{ app.satisfaction }}</b></div>
        <div style="margin-left: auto; display: flex; gap: 6px; align-items: center">
          <button v-if="canManage" class="btn btn-sm btn-quiet" type="button" @click="openEdit(app)">编辑</button>
          <button v-if="canManage" class="btn btn-sm btn-quiet" type="button" @click="remove(app)">删除</button>
          <RouterLink :to="app.route || '/apps'" class="btn btn-primary btn-sm">
            打开 <AppIcon name="arrow" :size="12" />
          </RouterLink>
        </div>
      </div>
    </article>

    <article class="app-card" style="opacity: 0.72; justify-content: center; text-align: center">
      <span class="ac-ico" style="margin: 0 auto"><AppIcon name="plus" :size="19" /></span>
      <h2 class="ac-name">更多 Agent 即将上线</h2>
      <p class="ac-desc" style="min-height: 0">
        催收合规问答、贷前尽调摘要正在灰度中。上线后将按部门与角色自动授权，无需单独申请。
      </p>
      <button class="btn btn-sm" type="button" @click="ui.toast('已登记关注，上线后会通过飞书通知你', 'ok')">
        关注上线通知
      </button>
    </article>
  </div>

  <div v-if="loading" class="hint mt14">加载中…</div>
  <div v-else-if="loadError" class="notice warn mt14">
    <AppIcon name="alert" :size="15" />
    <span>{{ loadError }}</span>
  </div>

  <Teleport to="body">
    <div v-if="formOpen" class="modal-backdrop" style="z-index: 120" @click.self="closeForm">
      <div class="modal" style="width: min(620px, 100%)" role="dialog" aria-modal="true">
        <div class="modal-head">
          <div>
            <h3>{{ editingCode ? '编辑 Agent 卡片' : '新增 Agent 卡片' }}</h3>
            <div class="mh-sub">卡片信息展示在「我的 Agent」页，Code 与后端应用标识保持一致</div>
          </div>
          <button class="btn btn-sm x" type="button" @click="closeForm">关闭</button>
        </div>
        <div class="modal-body" style="padding: 18px 20px">
          <div class="grid g2">
            <div class="field">
              <label class="field-label" for="app-name">名称<span class="req">*</span></label>
              <input id="app-name" v-model="form.name" class="input" type="text" placeholder="风控规则助手" />
            </div>
            <div class="field">
              <label class="field-label" for="app-code">Code<span class="req">*</span></label>
              <input
                id="app-code"
                v-model="form.code"
                class="input"
                type="text"
                placeholder="rule_qa_v3"
                :disabled="Boolean(editingCode)"
              />
            </div>
            <div class="field">
              <label class="field-label" for="app-icon">图标</label>
              <select id="app-icon" v-model="form.icon" class="select">
                <option v-for="icon in iconOptions" :key="icon" :value="icon">{{ icon }}</option>
              </select>
            </div>
            <div class="field">
              <label class="field-label" for="app-status">状态</label>
              <select id="app-status" v-model="form.status" class="select">
                <option v-for="status in statusOptions" :key="status" :value="status">
                  {{ statusMeta[status].text }}
                </option>
              </select>
            </div>
          </div>
          <div class="field">
            <label class="field-label" for="app-desc">描述</label>
            <textarea id="app-desc" v-model="form.desc" class="textarea" rows="3" placeholder="面向业务同学的解释型 Agent…"></textarea>
          </div>
          <div class="grid g3">
            <div class="field">
              <label class="field-label" for="app-calls">今日会话</label>
              <input id="app-calls" v-model.number="form.calls" class="input" type="number" min="0" />
            </div>
            <div class="field">
              <label class="field-label" for="app-latency">平均时延</label>
              <input id="app-latency" v-model="form.latency" class="input" type="text" placeholder="0.9s" />
            </div>
            <div class="field">
              <label class="field-label" for="app-satisfaction">满意度</label>
              <input id="app-satisfaction" v-model="form.satisfaction" class="input" type="text" placeholder="96%" />
            </div>
          </div>
          <div class="field">
            <label class="field-label" for="app-route">前端路由</label>
            <input id="app-route" v-model="form.route" class="input" type="text" placeholder="/apps/rule-qa" />
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
