package com.ggzed.lc4j.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 统一对话请求，字段与 yp-assistant-engine 的 ChatRequest 完全一致
 */
public record ModelRegistryRequest(
        @NotBlank(message = "消息内容不能为空")
        @Size(max = 4000, message = "消息内容长度不能超过 4000")
        String message,

        @NotBlank(message = "会话 ID 不能为空")
        @Size(max = 64, message = "会话 ID 长度不能超过 64")
        String sessionId) {
}
