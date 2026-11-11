/** 控制台各菜单的 CRUD 接口，统一走 /admin/**（经网关透传到 yp-agent-admin）。 */
import { del, get, post, put } from './http'

/** MyBatis-Plus 分页结构 */
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

type Query = Record<string, string | number | undefined | null>

function query(params: Query): string {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') search.set(key, String(value))
  })
  const text = search.toString()
  return text ? `?${text}` : ''
}

const enc = encodeURIComponent

/* ------------------------------ 告警提示 ------------------------------ */
export type AlertLevel = 'P1' | 'P2' | 'P3'
export type AlertState = '未处理' | '处理中' | '已恢复'

export interface AlertItem {
  id: number
  time: string
  level: AlertLevel
  title: string
  service: string
  impact: string
  state: AlertState
  read: boolean
  claimedBy?: string | null
}

export type AlertPayload = Omit<AlertItem, 'id' | 'read'> & { read?: boolean }

export function listAlerts(params: { page?: number; size?: number; state?: string; level?: string } = {}) {
  return get<PageResult<AlertItem>>(`/admin/alerts${query({ ...params })}`)
}

export function createAlert(payload: AlertPayload) {
  return post<AlertItem>('/admin/alerts', payload)
}

export function updateAlert(id: number, payload: AlertPayload) {
  return put<AlertItem>(`/admin/alerts/${id}`, payload)
}

export function deleteAlert(id: number) {
  return del<void>(`/admin/alerts/${id}`)
}

export function claimAlert(id: number, claimedBy?: string) {
  return post<AlertItem>(`/admin/alerts/${id}/claim${query({ claimedBy })}`, {})
}

export function readAllAlerts() {
  return post<void>('/admin/alerts/read-all', {})
}

/* ------------------------------ 我的 Agent ------------------------------ */
export type AppStatus = 'online' | 'gray' | 'offline'

export interface AppItem {
  id: number
  name: string
  code: string
  icon: string
  desc: string
  calls: number
  latency: string
  satisfaction: string
  status: AppStatus
  route: string
}

export type AppPayload = Omit<AppItem, 'id'>

export function listApps(params: { page?: number; size?: number; status?: string; keyword?: string } = {}) {
  return get<PageResult<AppItem>>(`/admin/apps${query({ ...params })}`)
}

export function createApp(payload: AppPayload) {
  return post<AppItem>('/admin/apps', payload)
}

export function updateApp(code: string, payload: AppPayload) {
  return put<AppItem>(`/admin/apps/${enc(code)}`, payload)
}

export function deleteApp(code: string) {
  return del<void>(`/admin/apps/${enc(code)}`)
}

/* ------------------------------ 工具目录 ------------------------------ */
export interface ToolItem {
  id: number
  name: string
  code: string
  cat: string
  proto: string
  status: string
  timeout: string
  latency: string
  risk: boolean
}

export type ToolPayload = Omit<ToolItem, 'id'>

export function listTools(
  params: { page?: number; size?: number; category?: string; keyword?: string; status?: string } = {}
) {
  return get<PageResult<ToolItem>>(`/admin/tools${query({ ...params })}`)
}

export function listToolCategories() {
  return get<string[]>('/admin/tools/categories')
}

export function createTool(payload: ToolPayload) {
  return post<ToolItem>('/admin/tools', payload)
}

export function updateTool(code: string, payload: ToolPayload) {
  return put<ToolItem>(`/admin/tools/${enc(code)}`, payload)
}

export function deleteTool(code: string) {
  return del<void>(`/admin/tools/${enc(code)}`)
}

/* ------------------------------ MCP Server ------------------------------ */
export interface McpServerItem {
  id: number
  name: string
  address: string
  protocol: string
  timeout: string
  fallback: string
  status: string
}

export type McpServerPayload = Omit<McpServerItem, 'id'>

export function listMcpServers(params: { page?: number; size?: number; status?: string; keyword?: string } = {}) {
  return get<PageResult<McpServerItem>>(`/admin/mcp-servers${query({ ...params })}`)
}

export function createMcpServer(payload: McpServerPayload) {
  return post<McpServerItem>('/admin/mcp-servers', payload)
}

export function updateMcpServer(name: string, payload: McpServerPayload) {
  return put<McpServerItem>(`/admin/mcp-servers/${enc(name)}`, payload)
}

export function deleteMcpServer(name: string) {
  return del<void>(`/admin/mcp-servers/${enc(name)}`)
}

/* ------------------------------ 知识库文档 ------------------------------ */
export interface KbDocumentItem {
  id: number
  name: string
  type: string
  size: string
  chunks: number
  status: string
  index: string
  by: string
}

export type KbDocumentPayload = Omit<KbDocumentItem, 'id'>

export function listKbDocuments(
  params: { page?: number; size?: number; index?: string; status?: string; keyword?: string } = {}
) {
  return get<PageResult<KbDocumentItem>>(`/admin/kb/documents${query({ ...params })}`)
}

export function createKbDocument(payload: KbDocumentPayload) {
  return post<KbDocumentItem>('/admin/kb/documents', payload)
}

export function updateKbDocument(id: number, payload: KbDocumentPayload) {
  return put<KbDocumentItem>(`/admin/kb/documents/${id}`, payload)
}

export function deleteKbDocument(id: number) {
  return del<void>(`/admin/kb/documents/${id}`)
}

export function retryKbDocument(id: number) {
  return post<KbDocumentItem>(`/admin/kb/documents/${id}/retry`, {})
}
