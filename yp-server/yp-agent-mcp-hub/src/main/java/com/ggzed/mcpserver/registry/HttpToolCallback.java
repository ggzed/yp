package com.ggzed.mcpserver.registry;

import com.ggzed.mcpserver.config.DynamicToolConfig;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.web.client.RestClient;

public class HttpToolCallback implements ToolCallback {

    private final ToolDefinition toolDefinition;
    private final DynamicToolConfig config;
    private final RestClient restClient;

    public HttpToolCallback(DynamicToolConfig config, RestClient restClient) {
        this.config = config;
        this.restClient = restClient;

        // 核心：手动构建大模型能看懂的工具定义（包含入参 JSON Schema）
        this.toolDefinition = ToolDefinition.builder()
                .name(config.getToolName())
                .description(config.getDescription())
                .inputSchema(config.getInputSchema())
                .build();
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return this.toolDefinition;
    }

    @Override
    public String call(String toolInput) {
        // 当大模型决定调用这个工具时，toolInput 就是模型生成的 JSON 字符串，比如 {"customerId":"VIP001"}
        System.out.println(">>> 动态工具被调用: " + config.getToolName() + "，参数: " + toolInput);

        try {
            // 这里为了简单，假设全是 POST JSON 请求（如果是 GET 需解析参数拼接）
            return restClient.post()
                    .uri(config.getTargetUrl())
                    .header("Content-Type", "application/json")
                    .body(toolInput)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            return "【系统提示】工具执行失败: " + e.getMessage();
        }
    }
}