import { ref } from 'vue'
import { defineStore } from 'pinia'
import { fetchMenus } from '@/api/menus'
import type { MenuGroup, MenuItem } from '@/api/menus'
import { useAuthStore } from '@/stores/auth'

/** 左侧菜单：登录后按用户角色从服务端拉取，角色切换时重新计算。 */
export const useMenuStore = defineStore('menu', () => {
  const groups = ref<MenuGroup[]>([])
  const loading = ref(false)
  const loaded = ref(false)
  const error = ref('')

  async function load() {
    const auth = useAuthStore()
    if (!auth.isAuthenticated) {
      groups.value = []
      loaded.value = false
      error.value = ''
      return
    }
    loading.value = true
    error.value = ''
    try {
      groups.value = await fetchMenus({ username: auth.profile?.username, role: auth.role })
      loaded.value = true
    } catch (err) {
      groups.value = []
      error.value = err instanceof Error ? err.message : '菜单加载失败'
    } finally {
      loading.value = false
    }
  }

  function groupOf(path: string): MenuGroup | undefined {
    return groups.value.find((group) => group.items.some((item) => item.path === path))
  }

  function itemOf(path: string): MenuItem | undefined {
    for (const group of groups.value) {
      const item = group.items.find((entry) => entry.path === path)
      if (item) return item
    }
    return undefined
  }

  function can(path: string, action: string): boolean {
    return itemOf(path)?.actions.includes(action) ?? false
  }

  return { groups, loading, loaded, error, load, groupOf, itemOf, can }
})
