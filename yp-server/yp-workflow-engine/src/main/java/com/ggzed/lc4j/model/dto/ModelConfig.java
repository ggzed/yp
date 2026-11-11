package com.ggzed.lc4j.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ModelConfig {
    private String provider;    // 例如: "openai", "ollama"
    private String baseUrl;     // 例如: "http://localhost:11434/v1"
    private String apiKey;      // 例如: "sk-xxx"
    private String modelName;   // 例如: "gpt-4o", "qwen2"
    private Double temperature; // 例如: 0.7
}
