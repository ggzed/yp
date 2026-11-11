/** 登录后按用户角色获取左侧菜单 */
import { get } from './http'

export interface MenuItem {
  path: string
  label: string
  badge?: string | null
  tag?: string | null
  /** 当前角色可执行的操作，如 view / manage / publish / use */
  actions: string[]
}

export interface MenuGroup {
  id: string
  label: string
  icon: string
  items: MenuItem[]
}

export function fetchMenus(params: { username?: string; role?: string } = {}) {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value) search.set(key, value)
  })
  const text = search.toString()
  return get<MenuGroup[]>(`/admin/menus${text ? `?${text}` : ''}`)
}
