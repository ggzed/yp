package com.ggzed.lc4j.controller;

import com.ggzed.lc4j.core.AgentKernel;
import com.ggzed.lc4j.result.Result;
import com.ggzed.lc4j.agent.FinancialAgent;
import com.ggzed.lc4j.model.dto.ChatResponse;
import com.ggzed.lc4j.model.request.ChatRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * LangChain4j 对话 API，路径与入参对齐 yp-assistant-engine
 */
@RestController
@RequestMapping("/api/chat")
public class Lc4jController {

    private final FinancialAgent financialAgent;

    private final AgentKernel agentKernel;

    public Lc4jController(FinancialAgent financialAgent,AgentKernel agentKernel) {
        this.financialAgent = financialAgent;
        this.agentKernel = agentKernel;
    }

    @PostMapping("/simple")
    public Result<ChatResponse> simple(@RequestBody @Valid ChatRequest request) {
        String content = financialAgent.chat(request.sessionId(), request.message());
        return Result.success(new ChatResponse(request.sessionId(), content));
    }

    @GetMapping("/chat")
    public Result<ChatResponse> chat(
            @RequestParam(defaultValue = "risk-agent") String agentId,
            @RequestParam String sessionId,
            @RequestParam String query) {

        // 所有请求统一进内核，由内核分发并执行
        String content =agentKernel.execute(agentId, sessionId, query);
        return Result.success(new ChatResponse(sessionId, content));
    }
}
