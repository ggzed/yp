import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

// 开发时把各服务的独立前缀代理到网关（8080）；没有启动网关时可改为对应服务端口
// 并同时把 VITE_CHAT_PREFIX 指向直连的问答引擎路径
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const proxyTarget = env.VITE_PROXY_TARGET || 'http://localhost:8080'

  const proxy = (target: string) => ({
    target,
    changeOrigin: true
  })

  return {
    plugins: [vue()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    },
    server: {
      port: 5173,
      proxy: {
        // 每个服务在网关上的独立前缀
        '/admin': proxy(proxyTarget),
        '/assistant': proxy(proxyTarget),
        '/workflow': proxy(proxyTarget),
        '/mcp': proxy(proxyTarget),
        '/business': proxy(proxyTarget)
      }
    }
  }
})
