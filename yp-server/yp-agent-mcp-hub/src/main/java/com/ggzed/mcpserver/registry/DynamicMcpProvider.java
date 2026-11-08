package com.ggzed.mcpserver.registry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ggzed.mcpserver.config.DynamicToolConfig;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态 MCP 工具注册中心。
 *
 * <p>把 Admin 后台配置的 HTTP 接口在运行时转换成 MCP 工具，整体分三步：
 * <ol>
 *   <li>用 {@link HttpToolCallback} 包装业务 HTTP 接口（工具真正被调用时由它发起请求）；</li>
 *   <li>把配置转成 MCP 协议层的 {@link McpServerFeatures.AsyncToolSpecification}，注册进 {@link McpAsyncServer}；</li>
 *   <li>注册/移除成功后广播 tools/list_changed 通知，让已连接的客户端（如 Apifox）刷新工具列表。</li>
 * </ol>
 *
 * <p>{@link McpAsyncServer} 通过 {@link ObjectProvider} 延迟获取：Spring AI 初始化
 * McpAsyncServer 时会反查所有 {@link ToolCallbackProvider}（即本类），直接注入会形成循环依赖。
 */
@Slf4j
@Component
public class DynamicMcpProvider implements ToolCallbackProvider {

    /** 已注册的动态工具，key 为工具名；用于实现 ToolCallbackProvider 接口供 Spring AI 读取 */
    private final Map<String, ToolCallback> dynamicTools = new ConcurrentHashMap<>();

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<McpAsyncServer> mcpServerProvider;

    public DynamicMcpProvider(ObjectProvider<McpAsyncServer> mcpServerProvider) {
        this.mcpServerProvider = mcpServerProvider;
        this.restClient = RestClient.create();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public ToolCallback[] getToolCallbacks() {
        log.debug("获取动态工具回调，当前数量: {}", dynamicTools.size());
        return dynamicTools.values().toArray(new ToolCallback[0]);
    }

    /**
     * 动态注册工具（同名工具会被 MCP SDK 覆盖）。
     *
     * @param config Admin 下发的工具配置
     * @return 注册并广播通知完成后才完成的 Mono，调用方必须订阅才会真正执行
     */
    public Mono<Void> registerTool(DynamicToolConfig config) {
        McpAsyncServer server = mcpServerProvider.getIfAvailable();
        if (server == null) {
            return Mono.error(new IllegalStateException(
                    "McpAsyncServer 未初始化（无状态模式下不支持动态增删工具），无法注册工具: " + config.getToolName()));
        }

        HttpToolCallback toolCallback = new HttpToolCallback(config, restClient);

        McpServerFeatures.AsyncToolSpecification specification;
        try {
            specification = buildSpecification(config, toolCallback);
        } catch (Exception e) {
            return Mono.error(new IllegalArgumentException("非法的 inputSchema JSON 字符串: " + config.getInputSchema(), e));
        }

        log.info("向 MCP Server 注册工具: {}", config.getToolName());
        return server.addTool(specification)
                .then(server.notifyToolsListChanged())
                .doOnSuccess(ignored -> {
                    // 注册成功后再写缓存，避免失败时留下一个不可用的工具
                    dynamicTools.put(config.getToolName(), toolCallback);
                    log.info("工具注册成功，已通知客户端刷新: {}", config.getToolName());
                });
    }

    /**
     * 动态移除工具。
     *
     * @param toolName 工具名
     * @return 移除并广播通知完成后才完成的 Mono，调用方必须订阅才会真正执行
     */
    public Mono<Void> removeTool(String toolName) {
        McpAsyncServer server = mcpServerProvider.getIfAvailable();
        if (server == null) {
            return Mono.error(new IllegalStateException(
                    "McpAsyncServer 未初始化（无状态模式下不支持动态增删工具），无法移除工具: " + toolName));
        }

        log.info("从 MCP Server 移除工具: {}", toolName);
        return server.removeTool(toolName)
                .then(server.notifyToolsListChanged())
                .doOnSuccess(ignored -> {
                    // 服务端移除成功后再清理本地缓存，保持两侧状态一致
                    dynamicTools.remove(toolName);
                    log.info("工具移除成功，已通知客户端刷新: {}", toolName);
                });
    }

    /**
     * 把 Admin 配置转换成 MCP 协议层的工具规格，同时完成 inputSchema 的合法性校验。
     *
     * @throws JsonProcessingException inputSchema 不是合法的 JSON 对象时抛出
     */
    private McpServerFeatures.AsyncToolSpecification buildSpecification(DynamicToolConfig config,
                                                                        HttpToolCallback toolCallback)
            throws JsonProcessingException {
        Map<String, Object> inputSchema = objectMapper.readValue(
                config.getInputSchema(), new TypeReference<Map<String, Object>>() { });

        McpSchema.Tool tool = McpSchema.Tool.builder()
                .name(config.getToolName())
                .description(config.getDescription())
                .inputSchema(inputSchema)
                .build();

        return McpServerFeatures.AsyncToolSpecification.builder()
                .tool(tool)
                .callHandler((exchange, request) -> invoke(toolCallback, request))
                .build();
    }

    /**
     * 工具被调用时的处理逻辑：把 MCP 入参序列化成 JSON 交给 HTTP 工具回调，再把返回文本包装成 CallToolResult。
     * 任何异常都转成 isError=true 的文本结果返回给大模型，不向上抛出。
     */
    private Mono<McpSchema.CallToolResult> invoke(HttpToolCallback toolCallback, McpSchema.CallToolRequest request) {
        String argumentsJson;
        try {
            argumentsJson = request.arguments() == null
                    ? "{}"
                    : objectMapper.writeValueAsString(request.arguments());
        } catch (JsonProcessingException e) {
            return Mono.just(errorResult("【系统提示】参数解析失败: " + e.getMessage()));
        }

        return Mono.fromCallable(() -> toolCallback.call(argumentsJson))
                .map(this::successResult);
    }

    private McpSchema.CallToolResult successResult(String text) {
        return McpSchema.CallToolResult.builder()
                .addTextContent(text)
                .build();
    }

    private McpSchema.CallToolResult errorResult(String message) {
        return McpSchema.CallToolResult.builder()
                .addTextContent(message)
                .isError(true)
                .build();
    }
}
