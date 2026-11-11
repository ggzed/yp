<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { AGENT_FILTERS } from '@/data/console'
import {
  deleteAgent,
  getAgent,
  listAgents,
  offlineAgent,
  publishAgent
} from '@/api/agents'
import type { AgentDetail, AgentListItem } from '@/api/agents'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'
import PageHead from '@/components/ui/PageHead.vue'
import PageTabs from '@/components/ui/PageTabs.vue'
import AppIcon from '@/components/ui/AppIcon.vue'
import AgentWizard from '@/components/business/AgentWizard.vue'

const auth = useAuthStore()
const ui = useUiStore()
const route = useRoute()

const tabs = [
  { label: 'Agent 列表', to: '/agents' },
  { label: '工作流编排', to: '/agents/flow' }
]

const filters = AGENT_FILTERS
const active = ref('全部')
const keyword = ref('')
const wizardOpen = ref(false)
const editing = ref<AgentDetail | null>(null)

const rows = ref<AgentListItem[]>([])
const loading = ref(false)
const loadError = ref('')

const statusTag: Record<string, string> = {
  已发布: 'tag-ok',
  草稿: 'tag-warn',
  已下线: 'tag-neutral'
}

function errorText(error: unknown) {
  return error instanceof Error ? error.message : '请求失败'
}

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    const page = await listAgents({
      page: 1,
      size: 100,
      status: active.value === '全部' ? undefined : active.value,
      keyword: keyword.value.trim() || undefined
    })
    rows.value = page.records
  } catch (error) {
    loadError.value = errorText(error)
  } finally {
    loading.value = false
  }
}

let timer: number | undefined
watch([active, keyword], () => {
  window.clearTimeout(timer)
  timer = window.setTimeout(load, 250)
})
onBeforeUnmount(() => window.clearTimeout(timer))

onMounted(() => {
  load()
  if (route.meta.autoWizard && auth.role === 'admin') openCreate()
})

function openCreate() {
  editing.value = null
  wizardOpen.value = true
}

async function openEdit(agent: AgentListItem) {
  try {
    editing.value = await getAgent(agent.code)
    wizardOpen.value = true
  } catch (error) {
    ui.toast(errorText(error), 'err')
  }
}

function closeWizard() {
  wizardOpen.value = false
  editing.value = null
}

async function onPublished() {
  const created = !editing.value
  closeWizard()
  await load()
  ui.toast(created ? '已添加到 Agent 列表' : 'Agent 配置已更新', 'ok')
}

async function toggleStatus(agent: AgentListItem) {
  try {
    if (agent.status === '已发布') {
      await offlineAgent(agent.code)
      ui.toast(`「${agent.name}」已下线，端点即刻不可调用`, 'ok')
    } else {
      await publishAgent(agent.code)
      ui.toast(`「${agent.name}」已重新发布`, 'ok')
    }
    await load()
  } catch (error) {
    ui.toast(errorText(error), 'err')
  }
}

async function remove(agent: AgentListItem) {
  if (!window.confirm(`确定删除 Agent「${agent.name}」？`)) return
  try {
    await deleteAgent(agent.code)
    ui.toast('Agent 已删除', 'ok')
    await load()
  } catch (error) {
    ui.toast(errorText(error), 'err')
  }
}
</script>

<template>
  <PageHead
    eyebrow="AGENTS / 智能体编排中心"
    title="Agent 列表"
    sub="管理已发布的智能体：配置提示词、知识库、工具与护栏，发布后写入 Nacos。"
  >
    <template #actions>
      <button class="btn" type="button" @click="ui.toast('已导出 Agent 清单（演示）', 'ok')">
        <AppIcon name="upload" :size="14" />导出
      </button>
      <button class="btn btn-primary" type="button" @click="openCreate">
        <AppIcon name="plus" :size="14" />新建 Agent
      </button>
    </template>
  </PageHead>

  <PageTabs :items="tabs" />

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
          placeholder="搜索名称 / Code"
          style="border: 0; background: transparent; outline: none; width: 100%; font-size: 11.5px"
          aria-label="搜索 Agent"
        />
      </div>
      <span class="panel-meta">{{ rows.length }} ITEMS</span>
    </header>

    <div class="panel-body" style="padding: 0; overflow-x: auto">
      <table class="tbl">
        <thead>
          <tr>
            <th>Agent</th>
            <th>类型</th>
            <th>模型</th>
            <th class="r">工具</th>
            <th class="r">今日调用</th>
            <th>状态</th>
            <th>负责人</th>
            <th>更新时间</th>
            <th class="r">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="agent in rows" :key="agent.code">
            <td>
              <div class="strong">{{ agent.name }}</div>
              <div class="sub">{{ agent.code }}</div>
            </td>
            <td>
              <span class="tag" :class="agent.type === '长链路型' ? 'tag-info' : 'tag-brand'">{{ agent.type }}</span>
            </td>
            <td class="mono" style="font-size: 11.5px">{{ agent.model }}</td>
            <td class="r num">{{ agent.tools }}</td>
            <td class="r num">{{ agent.calls.toLocaleString() }}</td>
            <td><span class="tag" :class="statusTag[agent.status] ?? 'tag-neutral'">{{ agent.status }}</span></td>
            <td>{{ agent.by }}</td>
            <td class="mono" style="font-size: 11px; color: var(--muted)">{{ agent.time }}</td>
            <td class="r" style="white-space: nowrap">
              <button class="btn btn-sm" type="button" @click="openEdit(agent)">
                <AppIcon name="edit" :size="12" />配置
              </button>
              <button class="btn btn-sm btn-quiet" type="button" @click="toggleStatus(agent)">
                {{ agent.status === '已发布' ? '下线' : '发布' }}
              </button>
              <button class="btn btn-sm btn-quiet" type="button" @click="remove(agent)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="loading" style="padding: 40px; text-align: center; color: var(--faint)">加载中…</div>
      <div v-else-if="loadError" style="padding: 40px; text-align: center; color: var(--danger)">{{ loadError }}</div>
      <div v-else-if="!rows.length" style="padding: 40px; text-align: center; color: var(--faint)">
        没有符合条件的 Agent，试试调整筛选或新建一个。
      </div>
    </div>
  </section>

  <AgentWizard :open="wizardOpen" :agent="editing" @close="closeWizard" @published="onPublished" />
</template>
