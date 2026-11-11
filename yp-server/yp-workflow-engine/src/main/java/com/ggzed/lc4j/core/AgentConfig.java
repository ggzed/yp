package com.ggzed.lc4j.core;

import com.ggzed.lc4j.model.dto.ModelConfig;
import lombok.Data;
import java.util.List;

// 从 Nacos/DB 读取的配置（这里先用模拟数据）
@Data
public class AgentConfig {
    private String agentId;
    private String systemPrompt;
    private String modelType;
    private List<String> allowedToolNames; // 👈 新增：该 Agent 允许使用的工具名列表
    private ModelConfig modelConfig;
}