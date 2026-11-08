package com.ggzed.lc4j.core;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.stereotype.Component;
// McpClient 接口：位于 client 子包
import dev.langchain4j.mcp.client.McpClient;

// DefaultMcpClient 实现类：同样位于 client 子包
import dev.langchain4j.mcp.client.DefaultMcpClient;

// McpToolProvider：位于 mcp 根包
import dev.langchain4j.mcp.McpToolProvider;

// StreamableHttpMcpTransport：位于 transport.http 子包
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AgentKernel {

    private final ChatModel mockChatModel; // 或者正常的 chatModel
    private final McpClient mcpClient; // 👈 注入 MCP Client，去连 8082 的 MCP Server
    private final McpToolProvider mcpToolProvider; // 👈 MCP 工具提供者

    // 缓存动态生成的代理对象
    private final Map<String, DynamicAssistant> agentCache = new ConcurrentHashMap<>();

    public AgentKernel(MockChatModel mockChatModel, McpClient mcpClient) {
        this.mockChatModel = mockChatModel;
        this.mcpClient = mcpClient;
        // 核心改造：基于 McpClient 构建工具提供者。
        // 它的作用就是自动去 MCP Server 拉取最新工具列表，并执行对应调用
        this.mcpToolProvider = McpToolProvider.builder()
                .mcpClients(mcpClient)
                .build();
    }

    /**
     * 内核执行入口
     */
    public String execute(String agentId, String sessionId, String userQuery) {
        DynamicAssistant assistant = agentCache.get(agentId);

        // 双重检查锁（DCL）防击穿
        if (assistant == null) {
            synchronized (this) {
                assistant = agentCache.get(agentId);
                if (assistant == null) {
                    assistant = buildAgent(agentId);
                    agentCache.put(agentId, assistant);
                }
            }
        }
        return assistant.chat(sessionId, userQuery);
    }

    /**
     * 动态构建 Agent 实例
     */
    private DynamicAssistant buildAgent(String agentId) {
        // 1. 获取配置
        AgentConfig config = getConfig(agentId);

        // 1. 构建 MCP 工具提供者，并加上过滤逻辑
        McpToolProvider toolProvider = McpToolProvider.builder()
                .mcpClients(mcpClient)
                // 👈 核心：根据当前 Agent 的允许列表，过滤掉不需要的工具
                .filterToolNames(config.getAllowedToolNames())
                .build();

        System.out.println(">>> 正在动态构建 Agent: " + agentId + "，可用工具数: " + config.getAllowedToolNames().size());


        // 2. 构建 Agent
        return AiServices.builder(DynamicAssistant.class)
                .chatModel(mockChatModel)
                .systemMessageProvider(memoryId -> config.getSystemPrompt())
                .toolProvider(toolProvider) // 只挂载过滤后的工具
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                        .id(memoryId).maxMessages(10).build())
                .build();
    }

    // 热更新：移除旧缓存，Admin 新增工具后调用此方法
    public void refreshAgent(String agentId) {
        agentCache.remove(agentId);
        System.out.println(">>> Agent 缓存已清除，下次请求将重新构建: " + agentId);
    }

    // 模拟配置获取
    private AgentConfig getConfig(String agentId) {
        AgentConfig config = new AgentConfig();
        config.setAgentId(agentId);

        // 根据 ID 分配不同的 Prompt
        if ("risk-agent".equals(agentId)) {
            config.setSystemPrompt("你是一个专业的银行风控专家，回答问题必须严谨。");
        } else {
            config.setSystemPrompt("你是一个通用客服助手。");
        }
        return config;
    }
}