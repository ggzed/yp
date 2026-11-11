<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import {
  claimAlert,
  createAlert,
  deleteAlert,
  listAlerts,
  readAllAlerts,
  updateAlert
} from '@/api/console'
import type { AlertItem, AlertLevel, AlertPayload, AlertState } from '@/api/console'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'
import PageHead from '@/components/ui/PageHead.vue'
import KpiCard from '@/components/ui/KpiCard.vue'
import AppIcon from '@/components/ui/AppIcon.vue'

const auth = useAuthStore()
const ui = useUiStore()

const filters = ['全部', '未处理', '处理中', '已恢复']
const active = ref('全部')

const rows = ref<AlertItem[]>([])
const loading = ref(false)
const loadError = ref('')

const levelTag = { P1: 'tag-danger', P2: 'tag-warn', P3: 'tag-info' } as const
const stateTag = { 未处理: 'tag-danger', 处理中: 'tag-warn', 已恢复: 'tag-ok' } as const
const canHandle = computed(() => auth.role !== 'ops')
const canManage = computed(() => auth.role === 'admin')

const activeCount = computed(() => rows.value.filter((row) => row.state !== '已恢复').length)
const todayCount = computed(() => {
  const now = new Date()
  return rows.value.filter((row) => {
    const date = new Date(row.time)
    return (
      date.getFullYear() === now.getFullYear() &&
      date.getMonth() === now.getMonth() &&
      date.getDate() === now.getDate()
    )
  }).length
})

function errorText(error: unknown) {
  return error instanceof Error ? error.message : '请求失败'
}

function fmtTime(value: string) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    const page = await listAlerts({
      page: 1,
      size: 100,
      state: active.value === '全部' ? undefined : active.value
    })
    rows.value = page.records
  } catch (error) {
    loadError.value = errorText(error)
  } finally {
    loading.value = false
  }
}

watch(active, load)
onMounted(load)

async function handle(row: AlertItem) {
  if (!canHandle.value) {
    ui.toast('运维/主管角色仅可查看告警，处理请转管理员', 'err')
    return
  }
  try {
    await claimAlert(row.id, auth.realName || auth.profile?.username || '管理员')
    ui.toast('已认领该告警，并通知值班同学', 'ok')
    await load()
  } catch (error) {
    ui.toast(errorText(error), 'err')
  }
}

async function markAllRead() {
  try {
    await readAllAlerts()
    ui.toast('已全部标记为已读', 'ok')
    await load()
  } catch (error) {
    ui.toast(errorText(error), 'err')
  }
}

/* ---------- 新建 / 编辑 ---------- */
const formOpen = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)
const form = ref<AlertPayload>(emptyForm())

function emptyForm(): AlertPayload {
  return {
    time: new Date().toISOString(),
    level: 'P3',
    title: '',
    service: '',
    impact: '',
    state: '未处理',
    read: false
  }
}

function openCreate() {
  editingId.value = null
  form.value = emptyForm()
  formOpen.value = true
}

function openEdit(row: AlertItem) {
  editingId.value = row.id
  form.value = {
    time: row.time,
    level: row.level,
    title: row.title,
    service: row.service,
    impact: row.impact,
    state: row.state,
    read: row.read,
    claimedBy: row.claimedBy
  }
  formOpen.value = true
}

function closeForm() {
  if (saving.value) return
  formOpen.value = false
}

async function save() {
  if (!form.value.title.trim()) {
    ui.toast('请填写告警事件', 'err')
    return
  }
  saving.value = true
  try {
    if (editingId.value !== null) {
      await updateAlert(editingId.value, form.value)
      ui.toast('告警已更新', 'ok')
    } else {
      await createAlert(form.value)
      ui.toast('告警已创建', 'ok')
    }
    formOpen.value = false
    await load()
  } catch (error) {
    ui.toast(errorText(error), 'err')
  } finally {
    saving.value = false
  }
}

async function remove(row: AlertItem) {
  if (!window.confirm(`确定删除告警「${row.title}」？`)) return
  try {
    await deleteAlert(row.id)
    ui.toast('告警已删除', 'ok')
    await load()
  } catch (error) {
    ui.toast(errorText(error), 'err')
  }
}

const levels: AlertLevel[] = ['P1', 'P2', 'P3']
const states: AlertState[] = ['未处理', '处理中', '已恢复']
</script>

<template>
  <PageHead eyebrow="DASHBOARD / 告警提示" title="告警与异常" sub="按严重级别收敛，P1 自动触发降级与值班通知。">
    <template #actions>
      <button class="btn" type="button" @click="markAllRead">全部标记已读</button>
      <button v-if="canManage" class="btn btn-primary" type="button" @click="openCreate">
        <AppIcon name="plus" :size="14" />新建告警
      </button>
      <button v-else class="btn btn-primary" type="button" @click="ui.toast('已跳转值班排班表（演示）', 'ok')">查看值班</button>
    </template>
  </PageHead>

  <div class="grid g4">
    <KpiCard label="活跃告警" :value="String(activeCount)" delta="含 P1 / P2" trend="up" variant="danger" :data="[1, 2, 2, 3, 2, 2, 1, 2, 2]" />
    <KpiCard label="今日新增" :value="String(todayCount)" delta="按告警时间统计" trend="down" variant="warn" :data="[6, 5, 5, 4, 5, 4, 4, 3, 4]" />
    <KpiCard label="平均恢复时长" value="18" unit="min" delta="-31%" trend="down" :data="[30, 28, 26, 24, 22, 21, 20, 19, 18]" />
    <KpiCard label="自动降级次数" value="7" delta="全部成功" trend="flat" variant="info" :data="[4, 5, 6, 5, 6, 7, 6, 7, 7]" />
  </div>

  <section class="panel mt14">
    <header class="panel-head" style="flex-wrap: wrap; gap: 12px">
      <h2 class="panel-title">告警列表</h2>
      <div class="chips" style="margin-left: 8px">
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
      <span class="panel-meta">共 {{ rows.length }} 条</span>
    </header>
    <div class="panel-body" style="padding: 0; overflow-x: auto">
      <table class="tbl">
        <thead>
          <tr>
            <th>时间</th>
            <th>级别</th>
            <th>事件</th>
            <th>影响服务</th>
            <th>影响范围</th>
            <th>状态</th>
            <th class="r">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in rows" :key="row.id">
            <td class="mono" style="white-space: nowrap">{{ fmtTime(row.time) }}</td>
            <td><span class="tag" :class="levelTag[row.level]">{{ row.level }}</span></td>
            <td class="strong">{{ row.title }}</td>
            <td class="mono" style="font-size: 11.5px">{{ row.service }}</td>
            <td>{{ row.impact }}</td>
            <td><span class="tag" :class="stateTag[row.state]">{{ row.state }}</span></td>
            <td class="r" style="white-space: nowrap">
              <button class="btn btn-sm" type="button" @click="handle(row)">处理</button>
              <button v-if="canManage" class="btn btn-sm btn-quiet" type="button" @click="openEdit(row)">编辑</button>
              <button v-if="canManage" class="btn btn-sm btn-quiet" type="button" @click="remove(row)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="loading" style="padding: 40px; text-align: center; color: var(--faint)">加载中…</div>
      <div v-else-if="loadError" style="padding: 40px; text-align: center; color: var(--danger)">{{ loadError }}</div>
      <div v-else-if="!rows.length" style="padding: 40px; text-align: center; color: var(--faint)">
        没有符合条件的告警。
      </div>
    </div>
  </section>

  <div class="notice mt14">
    <AppIcon name="shield" :size="15" />
    <span>P1 告警会同时通知值班主管与企业微信机器人；运维角色只读，不可直接修改告警状态。</span>
  </div>

  <Teleport to="body">
    <div v-if="formOpen" class="modal-backdrop" style="z-index: 120" @click.self="closeForm">
      <div class="modal" style="width: min(560px, 100%)" role="dialog" aria-modal="true">
        <div class="modal-head">
          <div>
            <h3>{{ editingId === null ? '新建告警' : '编辑告警' }}</h3>
            <div class="mh-sub">告警创建后可在列表中认领与恢复</div>
          </div>
          <button class="btn btn-sm x" type="button" @click="closeForm">关闭</button>
        </div>
        <div class="modal-body" style="padding: 18px 20px">
          <div class="field">
            <label class="field-label" for="alert-title">事件<span class="req">*</span></label>
            <input id="alert-title" v-model="form.title" class="input" type="text" placeholder="如 risk-rpc-core 响应超时率升至 4.2%" />
          </div>
          <div class="grid g2">
            <div class="field">
              <label class="field-label" for="alert-level">级别</label>
              <select id="alert-level" v-model="form.level" class="select">
                <option v-for="level in levels" :key="level" :value="level">{{ level }}</option>
              </select>
            </div>
            <div class="field">
              <label class="field-label" for="alert-state">状态</label>
              <select id="alert-state" v-model="form.state" class="select">
                <option v-for="state in states" :key="state" :value="state">{{ state }}</option>
              </select>
            </div>
            <div class="field">
              <label class="field-label" for="alert-service">影响服务</label>
              <input id="alert-service" v-model="form.service" class="input" type="text" placeholder="risk-rpc-core" />
            </div>
            <div class="field">
              <label class="field-label" for="alert-impact">影响范围</label>
              <input id="alert-impact" v-model="form.impact" class="input" type="text" placeholder="数据智算降级，影响 12 个会话" />
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
