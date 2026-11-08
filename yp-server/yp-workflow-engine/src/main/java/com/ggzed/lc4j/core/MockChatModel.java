package com.ggzed.lc4j.core;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary // 👈 确保微内核优先注入这个 Mock 实现
public class MockChatModel implements ChatModel {

    @Override
    public ChatResponse doChat(ChatRequest chatRequest) {
        try {
            // 模拟大模型延迟
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 返回 mock 响应
        return ChatResponse.builder()
                .aiMessage(new AiMessage("【Mock AI】模拟回答，未消耗真实 Token。"))
                .build();
    }
}