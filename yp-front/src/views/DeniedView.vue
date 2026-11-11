<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ROLES } from '@/data/console'
import { useAuthStore } from '@/stores/auth'
import { useMenuStore } from '@/stores/menu'
import AppIcon from '@/components/ui/AppIcon.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const menu = useMenuStore()

const from = computed(() => (typeof route.query.from === 'string' ? route.query.from : ''))
const target = computed(() => (from.value ? router.resolve(from.value) : null))
const targetTitle = computed(() => target.value?.meta.title ?? '该页面')
const requiredRoles = computed(() => {
  const roles = target.value?.meta.roles
  return roles ? roles.map((role) => ROLES[role].name).join(' / ') : '对应角色'
})
const allowedNow = computed(() => auth.allows(target.value?.meta.roles))

function home() {
  const first = menu.groups[0]?.items[0]
  router.push(first?.path ?? '/dashboard/overview')
}

function continueVisit() {
  if (from.value) router.push(from.value)
}
</script>

<template>
  <section class="denied">
    <div class="d-ico"><AppIcon name="lock" :size="22" /></div>
    <h3>当前角色无权访问{{ targetTitle }}</h3>
    <p>
      你正以「{{ ROLES[auth.role].name }}」身份访问。该页面仅对 <b>{{ requiredRoles }}</b> 开放，
      权限边界由平台统一配置。你可以切换角色，或返回有权限的首页。
    </p>
    <div style="display: flex; gap: 9px; justify-content: center; flex-wrap: wrap">
      <button class="btn" type="button" @click="home">返回首页</button>
      <button v-if="allowedNow && from" class="btn btn-primary" type="button" @click="continueVisit">
        继续访问 <AppIcon name="arrow" :size="13" />
      </button>
      <RouterLink v-else to="/dashboard/overview" class="btn btn-primary">
        回到系统总览
      </RouterLink>
    </div>
    <p class="hint" style="margin-top: 18px">
      提示：左下角角色切换器可在业务员、管理员、运维/主管之间切换，用于体验不同权限边界。
    </p>
  </section>
</template>
