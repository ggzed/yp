package com.ggzed.springai.ctl;

import com.ggzed.springai.common.result.Result;
import com.ggzed.springai.model.request.RagChatRequest;
import com.ggzed.springai.service.RagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * RAG 问答 API（知识库增强对话）
 */
@RestController
@RequestMapping("/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagService ragService;

    /**
     * RAG 非流式问答
     */
    @PostMapping("/chat")
    public Result<String> chat(@RequestBody @Valid RagChatRequest request) {
        return Result.success(ragService.chat(request));
    }

    /**
     * RAG 流式问答（SSE 打字机效果）
     */
    @PostMapping(value = "/chat_stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestBody @Valid RagChatRequest request) {
        return ragService.chatStream(request.getQuery(), request.getSessionId());
    }
}
