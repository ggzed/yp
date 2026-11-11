<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ROLES } from '@/data/console'
import { useAuthStore } from '@/stores/auth'
import { useMenuStore } from '@/stores/menu'
import { useUiStore } from '@/stores/ui'
import AppIcon from '@/components/ui/AppIcon.vue'

const auth = useAuthStore()
const menu = useMenuStore()
const ui = useUiStore()
const route = useRoute()
const router = useRouter()

function logout() {
  auth.logout()
  ui.toast('已退出登录', 'ok')
  router.replace({ name: 'login' })
}

const groupLabel = computed(() => {
  const path = route.path === '/agents/new' ? '/agents' : route.path
  return menu.groupOf(path)?.label ?? ''
})
const title = computed(() => route.meta.title ?? '')
const readOnly = computed(
  () =>
    auth.role === 'ops' &&
    ['/hub/tools', '/hub/servers', '/kb/docs', '/kb/recall'].includes(route.path)
)
</script>

<template>
  <header class="topbar">
    <button
      class="hamb"
      type="button"
      aria-label="展开导航"
      @click="ui.toggleSidebar()"
    >
      <AppIcon name="menu" :size="17" />
    </button>

    <nav class="crumbs" aria-label="面包屑">
      <span>{{ groupLabel }}</span>
      <span class="sep">/</span>
      <b aria-current="page">{{ title }}</b>
      <span v-if="readOnly" class="tag tag-neutral" style="margin-left: 6px">只读</span>
    </nav>

    <div class="top-actions">
      <button class="searchbox" type="button" @click="ui.toast('全局搜索为演示占位，可接入 ES 搜索')">
        <AppIcon name="search" :size="13" />
        <span>搜索 Agent / 工具 / 文档</span>
        <kbd>⌘K</kbd>
      </button>
      <span class="env-tag">PROD</span>
      <button class="icon-btn" type="button" aria-label="通知" @click="ui.toast('2 条待处理告警：risk-rpc-core 降级')">
        <AppIcon name="bell" :size="16" />
        <i class="rd"></i>
      </button>
      <div class="user-meta">
        <div class="avatar">{{ ROLES[auth.role].short }}</div>
        <div>
          <strong>{{ auth.realName || ROLES[auth.role].person.split(' · ')[0] }}</strong>
          <span>{{ ROLES[auth.role].name }}</span>
        </div>
      </div>
      <button class="icon-btn" type="button" aria-label="退出登录" title="退出登录" @click="logout">
        <AppIcon name="logout" :size="16" />
      </button>
    </div>
  </header>
</template>
