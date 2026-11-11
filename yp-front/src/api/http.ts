/** 轻量请求封装：开发环境经 Vite 代理到网关，生产同源部署。 */

export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

async function request<T>(url: string, init: RequestInit): Promise<T> {
  let resp: Response
  try {
    resp = await fetch(url, init)
  } catch {
    throw new Error('网络异常，请确认网关与管理后台已启动')
  }
  const json = (await resp.json().catch(() => null)) as ApiResult<T> | null
  if (!json) throw new Error(`服务响应异常（HTTP ${resp.status}）`)
  if (json.code !== 200) throw new Error(json.message || '请求失败')
  return json.data
}

export function post<T>(url: string, body: unknown): Promise<T> {
  return request<T>(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  })
}

export function get<T>(url: string): Promise<T> {
  return request<T>(url, { method: 'GET' })
}

export function put<T>(url: string, body: unknown): Promise<T> {
  return request<T>(url, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  })
}

export function del<T>(url: string): Promise<T> {
  return request<T>(url, { method: 'DELETE' })
}
