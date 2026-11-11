package com.ggzed.lc4j.core;

import com.ggzed.lc4j.llm.ModelFactory;
import com.ggzed.lc4j.model.dto.ModelConfig;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.mcp.client.McpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class AgentKernel {

    private final ModelFactory modelFactory; // 或者正常的 chatModel
    private final McpClient mcpClient; // 可能为空：yp-agent-mcp-hub 暂不可用时以无工具模式运行

    // 缓存动态生成的代理对象
    private final Map<String, DynamicAssistant> agentCache = new ConcurrentHashMap<>();
    // 缓存模型配置
    private final Map<String, ModelConfig> modelCache = new ConcurrentHashMap<>();
    // agent对应的方法集合
    private final Map<String, List<String>> allowedToolNames = new ConcurrentHashMap<>();

    public AgentKernel(ModelFactory modelFactory, ObjectProvider<McpClient> mcpClientProvider) {
        this.modelFactory = modelFactory;
        this.mcpClient = resolveMcpClient(mcpClientProvider);
    }

    /** MCP Server 未启动时不阻断应用启动，Agent 退化为无工具模式 */
    private McpClient resolveMcpClient(ObjectProvider<McpClient> provider) {
        try {
            McpClient client = provider.getIfAvailable();
            if (client == null) {
                log.warn("未配置 MCP Client，Agent 将以无工具模式运行");
            }
            return client;
        } catch (RuntimeException e) {
            log.warn("MCP 服务暂不可用，Agent 将以无工具模式运行: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 内核执行入口
     */
    public String execute(String agentId, String sessionId, String userQuery) {
        DynamicAssistant assistant = agentCache.get(agentId);
        // 1. 获取配置
        AgentConfig config = getConfig(agentId);
        // 双重检查锁（DCL）防击穿
        if (assistant == null) {
            synchronized (this) {
                assistant = agentCache.get(agentId);
                if (assistant == null) {
                    assistant = buildAgent(agentId,config);
                    agentCache.put(agentId, assistant);
                }
            }
        }
        return assistant.chat(sessionId, userQuery);
    }

    /**
     * 动态构建 Agent 实例
     */
    private DynamicAssistant buildAgent(String agentId, AgentConfig config) {
        ChatModel chatModel = modelFactory.getOrCreateModel(config.getModelConfig());
        // 2. 构建 Agent
        AiServices<DynamicAssistant> builder = AiServices.builder(DynamicAssistant.class)
                .chatModel(chatModel)
                .systemMessageProvider(memoryId -> config.getSystemPrompt())
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                        .id(memoryId).maxMessages(10).build());

        // 3. 挂载 MCP 工具：根据当前 Agent 的允许列表过滤；MCP 不可用时跳过
        if (mcpClient != null) {
            builder.toolProvider(McpToolProvider.builder()
                    .mcpClients(mcpClient)
                    .filterToolNames(config.getAllowedToolNames())
                    .build());
            System.out.println(">>> 正在动态构建 Agent: " + agentId
                    + "，可用工具数: " + config.getAllowedToolNames().size());
        } else {
            System.out.println(">>> 正在动态构建 Agent: " + agentId + "，MCP 未连接，未挂载工具");
        }

        return builder.build();
    }

    // 热更新：移除旧缓存，Admin 新增工具后调用此方法
    public void refreshAgent(String agentId) {
        agentCache.remove(agentId);
        System.out.println(">>> Agent 缓存已清除，下次请求将重新构建: " + agentId);
    }

    // 模拟配置获取
    private AgentConfig getConfig(String agentId) {
        ModelConfig modelConfig = ModelConfig.builder()
                .baseUrl("http://192.168.101.108:11434") // 关键：加上 /v1
                .apiKey("ollama")   // 占位符，Ollama 会忽略
                .provider("ollama")
                .modelName("qwen3:1.7b")              // 你本地 pull 的模型名
        .build();
        List<String> allowedToolNames = new ArrayList<>();
        AgentConfig config = new AgentConfig();
        config.setAgentId(agentId);
        config.setModelConfig(modelConfig);
        config.setAllowedToolNames(allowedToolNames);
        // 根据 ID 分配不同的 Prompt
        if ("risk-agent".equals(agentId)) {
            config.setSystemPrompt("你是一个专业的银行风控专家，回答问题必须严谨。");
        } else {
            config.setSystemPrompt("你是一个通用客服助手。");
        }
        return config;
    }
}