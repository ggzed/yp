package com.ggzed.mcpserver.controller;

import com.ggzed.mcpserver.config.DynamicToolConfig;
import com.ggzed.mcpserver.registry.DynamicMcpProvider;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal")
public class McpAdminController {

    private final DynamicMcpProvider mcpProvider;

    public McpAdminController(DynamicMcpProvider mcpProvider) {
        this.mcpProvider = mcpProvider;
    }

    // Admin 调用此接口，动态添加工具
    @PostMapping("/tools")
    public String addTool(@RequestBody DynamicToolConfig config) {
        // registerTool 返回的是惰性 Mono，必须订阅（block）才会真正向 MCP Server 注册
        mcpProvider.registerTool(config).block();
        // 实际生产中，这里还会把 config 落库到 MySQL，防止 MCP Server 重启丢失
        return "工具 " + config.getToolName() + " 动态注册成功！";
    }
}