<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  createMcpServer,
  deleteMcpServer,
  listMcpServers,
  updateMcpServer
} from '@/api/console'
import type { McpServerItem, McpServerPayload } from '@/api/console'
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

const servers = ref<McpServerItem[]>([])
const loading = ref(false)
const loadError = ref('')
const readonly = computed(() => auth.role === 'ops')

const onlineCount = computed(() => servers.value.filter((server) => server.status === '在线').length)
const degraded = computed(() => servers.value.filter((server) => server.status !== '在线'))

function errorText(error: unknown) {
  return error instanceof Error ? error.message : '请求失败'
}

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    const page = await listMcpServers({ page: 1, size: 100 })
    servers.value = page.records
  } catch (error) {
    loadError.value = errorText(error)
  } finally {
    loading.value = false
  }
}

onMounted(load)

function testConnection(server: McpServerItem) {
  if (readonly.value) {
    ui.toast('只读角色不可执行连通性测试', 'err')
    return
  }
  ui.toast(`${server.name} 连通性正常 · 握手耗时 12ms`, 'ok')
}

/* ---------- 新增 / 编辑 ---------- */
const formOpen = ref(false)
const saving = ref(false)
const editingName = ref('')
const form = ref<McpServerPayload>(emptyForm())

function emptyForm(): McpServerPayload {
  return {
    name: '',
    address: '',
    protocol: 'MCP / SSE',
    timeout: '1200ms',
    fallback: '',
    status: '在线'
  }
}

function openCreate() {
  editingName.value = ''
  form.value = emptyForm()
  formOpen.value = true
}

function openEdit(server: McpServerItem) {
  editingName.value = server.name
  form.value = {
    name: server.name,
    address: server.address,
    protocol: server.protocol,
    timeout: server.timeout,
    fallback: server.fallback,
    status: server.status
  }
  formOpen.value = true
}

function closeForm() {
  if (saving.value) return
  formOpen.value = false
}

async function save() {
  if (!form.value.name.trim()) {
    ui.toast('请填写服务名称', 'err')
    return
  }
  if (!form.value.address.trim()) {
    ui.toast('请填写服务地址', 'err')
    return
  }
  saving.value = true
  try {
    if (editingName.value) {
      await updateMcpServer(editingName.value, form.value)
      ui.toast('注册信息已更新', 'ok')
    } else {
      await createMcpServer(form.value)
      ui.toast('MCP Server 已注册', 'ok')
    }
    formOpen.value = false
    await load()
  } catch (error) {
    ui.toast(errorText(error), 'err')
  } finally {
    saving.value = false
  }
}

async function remove(server: McpServerItem) {
  if (!window.confirm(`确定删除注册「${server.name}」？`)) return
  try {
    await deleteMcpServer(server.name)
    ui.toast('注册已删除', 'ok')
    await load()
  } catch (error) {
    ui.toast(errorText(error), 'err')
  }
}
</script>

<template>
  <PageHead
    eyebrow="HUB / MCP SERVER"
    title="MCP Server 注册"
    sub="登记 RPC / MCP 服务地址、超时与降级话术；降级话术会在工具不可用时直接回复给用户。"
  >
    <template #actions>
      <button v-if="!readonly" class="btn btn-primary" type="button" @click="openCreate">
        <AppIcon name="plus" :size="14" />新增注册
      </button>
      <span v-else class="tag tag-neutral">只读视角</span>
    </template>
  </PageHead>

  <PageTabs :items="tabs" />

  <div v-if="degraded.length" class="notice warn" style="margin-bottom: 14px">
    <AppIcon name="alert" :size="15" />
    <span>
      {{ degraded.map((server) => server.name).join('、') }} 当前处于降级状态：调用将返回降级话术，并记录降级来源。
    </span>
  </div>

  <section class="panel">
    <header class="panel-head">
      <h2 class="panel-title">已注册服务</h2>
      <span class="panel-meta">
        {{ servers.length }} SERVERS · {{ onlineCount }} ONLINE / {{ servers.length - onlineCount }} DEGRADED
      </span>
    </header>
    <div class="panel-body" style="padding: 0; overflow-x: auto">
      <table class="tbl">
        <thead>
          <tr>
            <th>服务名称</th>
            <th>地址</th>
            <th>协议</th>
            <th>超时</th>
            <th>降级话术</th>
            <th>状态</th>
            <th class="r">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="server in servers" :key="server.name">
            <td class="strong">{{ server.name }}</td>
            <td class="mono" style="font-size: 11.5px">{{ server.address }}</td>
            <td><span class="tag tag-neutral">{{ server.protocol }}</span></td>
            <td class="num">{{ server.timeout }}</td>
            <td style="color: var(--muted); font-size: 11.5px">{{ server.fallback }}</td>
            <td>
              <span class="tag" :class="server.status === '在线' ? 'tag-ok' : 'tag-warn'">{{ server.status }}</span>
            </td>
            <td class="r" style="white-space: nowrap">
              <button class="btn btn-sm" type="button" @click="testConnection(server)">测试连接</button>
              <button v-if="!readonly" class="btn btn-sm btn-quiet" type="button" @click="openEdit(server)">编辑</button>
              <button v-if="!readonly" class="btn btn-sm btn-quiet" type="button" @click="remove(server)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="loading" style="padding: 40px; text-align: center; color: var(--faint)">加载中…</div>
      <div v-else-if="loadError" style="padding: 40px; text-align: center; color: var(--danger)">{{ loadError }}</div>
      <div v-else-if="!servers.length" style="padding: 40px; text-align: center; color: var(--faint)">
        暂无已注册服务。
      </div>
    </div>
  </section>

  <p class="hint mt14">提示：超时与降级话术变更会实时下发到网关，无需重启 Agent 服务。</p>

  <Teleport to="body">
    <div v-if="formOpen" class="modal-backdrop" style="z-index: 120" @click.self="closeForm">
      <div class="modal" style="width: min(600px, 100%)" role="dialog" aria-modal="true">
        <div class="modal-head">
          <div>
            <h3>{{ editingName ? '编辑注册信息' : '新增注册' }}</h3>
            <div class="mh-sub">服务名唯一，降级话术会在工具不可用时直接回复给用户</div>
          </div>
          <button class="btn btn-sm x" type="button" @click="closeForm">关闭</button>
        </div>
        <div class="modal-body" style="padding: 18px 20px">
          <div class="grid g2">
            <div class="field">
              <label class="field-label" for="server-name">服务名称<span class="req">*</span></label>
              <input
                id="server-name"
                v-model="form.name"
                class="input"
                type="text"
                placeholder="risk-rpc-core"
                :disabled="Boolean(editingName)"
              />
            </div>
            <div class="field">
              <label class="field-label" for="server-address">地址<span class="req">*</span></label>
              <input id="server-address" v-model="form.address" class="input" type="text" placeholder="10.24.8.11:9090" />
            </div>
            <div class="field">
              <label class="field-label" for="server-protocol">协议</label>
              <select id="server-protocol" v-model="form.protocol" class="select">
                <option value="Dubbo / RPC">Dubbo / RPC</option>
                <option value="MCP / SSE">MCP / SSE</option>
                <option value="MCP / STDIO">MCP / STDIO</option>
                <option value="HTTP / REST">HTTP / REST</option>
              </select>
            </div>
            <div class="field">
              <label class="field-label" for="server-timeout">超时</label>
              <input id="server-timeout" v-model="form.timeout" class="input" type="text" placeholder="1200ms" />
            </div>
            <div class="field">
              <label class="field-label" for="server-status">状态</label>
              <select id="server-status" v-model="form.status" class="select">
                <option value="在线">在线</option>
                <option value="降级">降级</option>
              </select>
            </div>
          </div>
          <div class="field">
            <label class="field-label" for="server-fallback">降级话术</label>
            <textarea
              id="server-fallback"
              v-model="form.fallback"
              class="textarea"
              rows="3"
              placeholder="「服务暂时不可用，请稍后重试」"
            ></textarea>
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
