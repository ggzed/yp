/** 编排引擎对话：经 Vite 代理 /workflow 前缀到网关，再由网关转发到 yp-workflow-engine */
import { get } from './http'

export interface WorkflowChatResponse {
  sessionId: string
  content: string
}

export function chatWithWorkflow(params: { agentId: string; sessionId: string; query: string }) {
  const search = new URLSearchParams({
    agentId: params.agentId,
    sessionId: params.sessionId,
    query: params.query
  })
  return get<WorkflowChatResponse>(`/workflow/chat/chat?${search.toString()}`)
}
