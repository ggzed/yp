<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  createKbDocument,
  deleteKbDocument,
  listKbDocuments,
  retryKbDocument,
  updateKbDocument
} from '@/api/console'
import type { KbDocumentItem, KbDocumentPayload } from '@/api/console'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'
import PageHead from '@/components/ui/PageHead.vue'
import PageTabs from '@/components/ui/PageTabs.vue'
import AppIcon from '@/components/ui/AppIcon.vue'

const auth = useAuthStore()
const ui = useUiStore()

const tabs = [
  { label: '文档列表', to: '/kb/docs' },
  { label: '召回测试', to: '/kb/recall' }
]

const docs = ref<KbDocumentItem[]>([])
const loading = ref(false)
const loadError = ref('')
const readonly = computed(() => auth.role === 'ops')

const statusTag: Record<string, string> = {
  已向量化: 'tag-ok',
  解析中: 'tag-warn',
  解析失败: 'tag-danger'
}

function errorText(error: unknown) {
  return error instanceof Error ? error.message : '请求失败'
}

function today() {
  const now = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
}

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    const page = await listKbDocuments({ page: 1, size: 100 })
    docs.value = page.records
  } catch (error) {
    loadError.value = errorText(error)
  } finally {
    loading.value = false
  }
}

onMounted(load)

/* ---------- 上传 / 编辑 ---------- */
const formOpen = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)
const form = ref<KbDocumentPayload>(emptyForm())

function emptyForm(): KbDocumentPayload {
  return {
    name: '',
    type: 'PDF',
    size: '',
    chunks: 0,
    status: '解析中',
    index: '—',
    by: today()
  }
}

function openCreate() {
  if (readonly.value) {
    ui.toast('只读角色不可上传文档', 'err')
    return
  }
  editingId.value = null
  form.value = emptyForm()
  formOpen.value = true
}

function openEdit(doc: KbDocumentItem) {
  editingId.value = doc.id
  form.value = {
    name: doc.name,
    type: doc.type,
    size: doc.size,
    chunks: doc.chunks,
    status: doc.status,
    index: doc.index,
    by: doc.by
  }
  formOpen.value = true
}

function closeForm() {
  if (saving.value) return
  formOpen.value = false
}

async function save() {
  if (!form.value.name.trim()) {
    ui.toast('请填写文档名称', 'err')
    return
  }
  saving.value = true
  try {
    if (editingId.value !== null) {
      await updateKbDocument(editingId.value, form.value)
      ui.toast('文档信息已更新', 'ok')
    } else {
      await createKbDocument(form.value)
      ui.toast('文档已上传，正在解析与向量化…', 'ok')
    }
    formOpen.value = false
    await load()
  } catch (error) {
    ui.toast(errorText(error), 'err')
  } finally {
    saving.value = false
  }
}

async function retry(doc: KbDocumentItem) {
  if (readonly.value) {
    ui.toast('只读角色不可操作文档', 'err')
    return
  }
  try {
    await retryKbDocument(doc.id)
    ui.toast('已重新解析该文档', 'ok')
    await load()
  } catch (error) {
    ui.toast(errorText(error), 'err')
  }
}

async function remove(doc: KbDocumentItem) {
  if (!window.confirm(`确定删除文档「${doc.name}」？`)) return
  try {
    await deleteKbDocument(doc.id)
    ui.toast('文档已删除', 'ok')
    await load()
  } catch (error) {
    ui.toast(errorText(error), 'err')
  }
}
</script>

<template>
  <PageHead
    eyebrow="KNOWLEDGE / 文档列表"
    title="知识库文档"
    sub="上传、解析、切片与向量化。解析完成后可在「召回测试」中验证命中效果。"
  >
    <template #actions>
      <button v-if="!readonly" class="btn btn-primary" type="button" @click="openCreate">
        <AppIcon name="upload" :size="14" />上传文档
      </button>
      <span v-else class="tag tag-neutral">只读视角</span>
    </template>
  </PageHead>

  <PageTabs :items="tabs" />

  <div v-if="readonly" class="notice" style="margin-bottom: 14px">
    <AppIcon name="shield" :size="15" />
    <span>运维/主管角色可查看文档与解析状态；上传、重试与删除仅管理员可用。</span>
  </div>

  <div v-if="!readonly" class="uploader" style="margin-bottom: 14px">
    <AppIcon class="u-ico" name="upload" :size="26" />
    <div style="font-weight: 600; color: var(--ink-2)">拖拽 PDF / Word / CSV 到此处，或点击上传</div>
    <div class="hint" style="margin-top: 4px">单文件 ≤ 20 MB · 自动切片并写入 risk_rules_v3 索引</div>
    <button class="btn btn-primary btn-sm" type="button" style="margin-top: 12px" @click="openCreate">选择文件</button>
  </div>

  <section class="panel">
    <header class="panel-head">
      <h2 class="panel-title">全部文档</h2>
      <span class="panel-meta">{{ docs.length }} DOCS</span>
    </header>
    <div class="panel-body" style="padding: 0; overflow-x: auto">
      <table class="tbl">
        <thead>
          <tr>
            <th>文档</th>
            <th>类型</th>
            <th>大小</th>
            <th class="r">切片</th>
            <th>状态</th>
            <th>索引库</th>
            <th>上传时间</th>
            <th class="r">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="doc in docs" :key="doc.id">
            <td>
              <div class="strong" style="display: flex; align-items: center; gap: 8px">
                <AppIcon name="file" :size="14" style="color: var(--brand)" />
                {{ doc.name }}
              </div>
            </td>
            <td><span class="tag tag-neutral">{{ doc.type }}</span></td>
            <td class="num">{{ doc.size }}</td>
            <td class="r num">{{ doc.chunks || '—' }}</td>
            <td>
              <span class="tag" :class="statusTag[doc.status] ?? 'tag-neutral'">{{ doc.status }}</span>
              <div v-if="doc.status === '解析中'" class="progress" style="margin-top: 5px">
                <i style="width: 62%"></i>
              </div>
            </td>
            <td class="mono" style="font-size: 11px">{{ doc.index }}</td>
            <td class="num">{{ doc.by }}</td>
            <td class="r" style="white-space: nowrap">
              <button v-if="doc.status === '解析失败' && !readonly" class="btn btn-sm" type="button" @click="retry(doc)">重试</button>
              <button v-if="!readonly" class="btn btn-sm btn-quiet" type="button" @click="openEdit(doc)">编辑</button>
              <button v-if="!readonly" class="btn btn-sm btn-quiet" type="button" @click="remove(doc)">删除</button>
              <button
                class="btn btn-sm btn-quiet"
                type="button"
                @click="ui.toast(doc.status === '解析中' ? '文档仍在解析中，请稍候' : `已打开「${doc.name}」的切片预览（演示）`, doc.status === '解析中' ? 'err' : '')"
              >
                切片预览
              </button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="loading" style="padding: 40px; text-align: center; color: var(--faint)">加载中…</div>
      <div v-else-if="loadError" style="padding: 40px; text-align: center; color: var(--danger)">{{ loadError }}</div>
      <div v-else-if="!docs.length" style="padding: 40px; text-align: center; color: var(--faint)">暂无文档，试试上传一个。</div>
    </div>
  </section>

  <Teleport to="body">
    <div v-if="formOpen" class="modal-backdrop" style="z-index: 120" @click.self="closeForm">
      <div class="modal" style="width: min(580px, 100%)" role="dialog" aria-modal="true">
        <div class="modal-head">
          <div>
            <h3>{{ editingId === null ? '上传文档' : '编辑文档' }}</h3>
            <div class="mh-sub">文档登记后会进入解析队列，解析完成写入索引库</div>
          </div>
          <button class="btn btn-sm x" type="button" @click="closeForm">关闭</button>
        </div>
        <div class="modal-body" style="padding: 18px 20px">
          <div class="field">
            <label class="field-label" for="doc-name">文档名称<span class="req">*</span></label>
            <input id="doc-name" v-model="form.name" class="input" type="text" placeholder="个人信贷风控规则手册_v3.2.pdf" />
          </div>
          <div class="grid g2">
            <div class="field">
              <label class="field-label" for="doc-type">类型</label>
              <select id="doc-type" v-model="form.type" class="select">
                <option value="PDF">PDF</option>
                <option value="WORD">WORD</option>
                <option value="CSV">CSV</option>
                <option value="TXT">TXT</option>
              </select>
            </div>
            <div class="field">
              <label class="field-label" for="doc-size">大小</label>
              <input id="doc-size" v-model="form.size" class="input" type="text" placeholder="4.2 MB" />
            </div>
            <div class="field">
              <label class="field-label" for="doc-status">状态</label>
              <select id="doc-status" v-model="form.status" class="select">
                <option value="解析中">解析中</option>
                <option value="已向量化">已向量化</option>
                <option value="解析失败">解析失败</option>
              </select>
            </div>
            <div class="field">
              <label class="field-label" for="doc-chunks">切片数</label>
              <input id="doc-chunks" v-model.number="form.chunks" class="input" type="number" min="0" />
            </div>
            <div class="field">
              <label class="field-label" for="doc-index">索引库</label>
              <input id="doc-index" v-model="form.index" class="input" type="text" placeholder="risk_rules_v3" />
            </div>
            <div class="field">
              <label class="field-label" for="doc-by">上传人 / 日期</label>
              <input id="doc-by" v-model="form.by" class="input" type="text" placeholder="11-06" />
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
