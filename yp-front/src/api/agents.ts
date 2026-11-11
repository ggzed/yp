/** Agent 列表与配置接口，对应 yp-agent-admin 的 /admin/agents（F-15 ~ F-22）。 */
import { del, get, post, put } from './http'
import type { PageResult } from './console'

/** 列表项（AgentItemVO） */
export interface AgentListItem {
  name: string
  code: string
  type: string
  model: string
  status: string
  tools: number
  calls: number
  by: string
  time: string
}

/** 完整配置（SysAgent） */
export interface AgentDetail {
  id?: number
  name: string
  code: string
  type: string
  desc?: string
  dept?: string
  model?: string
  temp?: number
  topP?: number
  prompt?: string
  kb?: string
  topK?: number
  threshold?: number
  hybrid?: boolean
  memory?: boolean
  rounds?: number
  tools?: string[]
  guards?: string[]
  quota?: number
  note?: string
  status?: string
  owner?: string
  calls?: number
}

export interface AgentPublishResult {
  dataId: string
  nacosDataId: string
  endpoint: string
  effectiveIn: string
}

export function listAgents(params: { page?: number; size?: number; status?: string; keyword?: string } = {}) {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') search.set(key, String(value))
  })
  const text = search.toString()
  return get<PageResult<AgentListItem>>(`/admin/agents${text ? `?${text}` : ''}`)
}

export function getAgent(code: string) {
  return get<AgentDetail>(`/admin/agents/${encodeURIComponent(code)}`)
}

export function createAgent(payload: AgentDetail) {
  return post<AgentDetail>('/admin/agents', payload)
}

export function updateAgent(code: string, payload: AgentDetail) {
  return put<AgentDetail>(`/admin/agents/${encodeURIComponent(code)}`, payload)
}

export function deleteAgent(code: string) {
  return del<void>(`/admin/agents/${encodeURIComponent(code)}`)
}

export function publishAgent(code: string) {
  return post<AgentPublishResult>(`/admin/agents/${encodeURIComponent(code)}/publish`, {})
}

export function offlineAgent(code: string) {
  return post<void>(`/admin/agents/${encodeURIComponent(code)}/offline`, {})
}
