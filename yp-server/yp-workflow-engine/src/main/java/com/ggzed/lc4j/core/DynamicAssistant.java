package com.ggzed.lc4j.core;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;

// 统一的 Agent 代理接口，不需要加 @AiService 注解
public interface DynamicAssistant {
    String chat(@MemoryId String sessionId, @UserMessage String userMessage);
}