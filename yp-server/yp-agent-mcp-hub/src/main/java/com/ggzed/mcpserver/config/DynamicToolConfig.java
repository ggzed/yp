package com.ggzed.mcpserver.config;

import lombok.Data;

@Data
public class DynamicToolConfig {
    private String toolName;          // 工具名，如 "queryCreditScore"
    private String description;       // 工具描述，如 "查询客户征信分数"
    private String targetUrl;         // 底层业务接口的 URL
    private String httpMethod;        // GET / POST
    // 这个就是 Admin 界面配置的 JSON Schema（出入参结构）
    private String inputSchema;
}