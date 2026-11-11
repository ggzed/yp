import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { ALL_ROLES } from '@/data/console'
import type { RoleKey } from '@/data/console'
import { useAuthStore } from '@/stores/auth'

import OverviewView from '@/views/OverviewView.vue'
import AlertInboxView from '@/views/AlertInboxView.vue'
import AppCenterView from '@/views/AppCenterView.vue'
import ChatView from '@/views/ChatView.vue'
import AgentListView from '@/views/AgentListView.vue'
import AgentFlowView from '@/views/AgentFlowView.vue'
import ToolCatalogView from '@/views/ToolCatalogView.vue'
import ServerRegistryView from '@/views/ServerRegistryView.vue'
import RpcDebuggerView from '@/views/RpcDebuggerView.vue'
import DocumentListView from '@/views/DocumentListView.vue'
import RecallPlaygroundView from '@/views/RecallPlaygroundView.vue'
import BillingView from '@/views/BillingView.vue'
import TraceView from '@/views/TraceView.vue'
import ComplianceView from '@/views/ComplianceView.vue'
import DeniedView from '@/views/DeniedView.vue'
import LoginView from '@/views/LoginView.vue'

declare module 'vue-router' {
  interface RouteMeta {
    title: string
    roles?: readonly RoleKey[]
    mode?: 'rule' | 'data' | 'workflow'
    autoWizard?: boolean
  }
}

const routes: RouteRecordRaw[] = [
  { path: '/', redirect: '/dashboard/overview' },
  {
    path: '/login',
    name: 'login',
    component: LoginView,
    meta: { title: '登录' }
  },
  {
    path: '/dashboard/overview',
    name: 'overview',
    component: OverviewView,
    meta: { title: '系统总览', roles: ALL_ROLES }
  },
  {
    path: '/dashboard/alerts',
    name: 'alerts',
    component: AlertInboxView,
    meta: { title: '告警提示', roles: ['admin', 'ops'] }
  },
  {
    path: '/apps',
    name: 'app-center',
    component: AppCenterView,
    meta: { title: '我的 Agent', roles: ['sales', 'admin'] }
  },
  {
    path: '/apps/rule-qa',
    name: 'rule-qa',
    component: ChatView,
    meta: { title: '风控规则助手', roles: ['sales', 'admin'], mode: 'rule' }
  },
  {
    path: '/apps/analytics',
    name: 'analytics',
    component: ChatView,
    meta: { title: '风控数据智算', roles: ['sales', 'admin'], mode: 'data' }
  },
  {
    path: '/apps/workflow-chat',
    name: 'workflow-chat',
    component: ChatView,
    meta: { title: '工作流对话', roles: ['sales', 'admin'], mode: 'workflow' }
  },
  {
    path: '/agents',
    name: 'agent-list',
    component: AgentListView,
    meta: { title: 'Agent 列表', roles: ['admin'] }
  },
  {
    path: '/agents/new',
    name: 'agent-new',
    component: AgentListView,
    meta: { title: '新建智能体', roles: ['admin'], autoWizard: true }
  },
  {
    path: '/agents/flow',
    name: 'agent-flow',
    component: AgentFlowView,
    meta: { title: '工作流编排', roles: ['admin'] }
  },
  {
    path: '/hub/tools',
    name: 'hub-tools',
    component: ToolCatalogView,
    meta: { title: '工具分类目录', roles: ['admin', 'ops'] }
  },
  {
    path: '/hub/servers',
    name: 'hub-servers',
    component: ServerRegistryView,
    meta: { title: 'MCP Server 注册', roles: ['admin', 'ops'] }
  },
  {
    path: '/hub/debug',
    name: 'hub-debug',
    component: RpcDebuggerView,
    meta: { title: '接口调试', roles: ['admin'] }
  },
  {
    path: '/kb/docs',
    name: 'kb-docs',
    component: DocumentListView,
    meta: { title: '文档列表', roles: ['admin', 'ops'] }
  },
  {
    path: '/kb/recall',
    name: 'kb-recall',
    component: RecallPlaygroundView,
    meta: { title: '召回测试', roles: ['admin', 'ops'] }
  },
  {
    path: '/audit/billing',
    name: 'audit-billing',
    component: BillingView,
    meta: { title: 'Token 计费大盘', roles: ['admin', 'ops'] }
  },
  {
    path: '/audit/trace',
    name: 'audit-trace',
    component: TraceView,
    meta: { title: '全链路追踪', roles: ['admin', 'ops'] }
  },
  {
    path: '/audit/compliance',
    name: 'audit-compliance',
    component: ComplianceView,
    meta: { title: '合规审计', roles: ['ops'] }
  },
  {
    path: '/denied',
    name: 'denied',
    component: DeniedView,
    meta: { title: '暂无访问权限' }
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard/overview' }
]

export const router = createRouter({
  history: createWebHistory(),
  routes
})

// 认证守卫：未登录统一去登录页并携带回跳地址；登录后按 RBAC 校验，越权落到 403 页
router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.name === 'login') {
    return auth.isAuthenticated ? { name: 'overview' } : true
  }
  if (!auth.isAuthenticated) {
    return { name: 'login', query: to.fullPath === '/' ? {} : { redirect: to.fullPath } }
  }
  const roles = to.meta.roles
  if (roles && !roles.includes(auth.role)) {
    return { name: 'denied', query: { from: to.fullPath } }
  }
  return true
})
