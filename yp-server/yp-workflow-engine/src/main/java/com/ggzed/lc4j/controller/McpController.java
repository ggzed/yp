//package com.ggzed.lc4j.controller;
//
//import com.ggzed.lc4j.core.AgentKernel;
//import com.ggzed.lc4j.model.dto.ChatResponse;
//import com.ggzed.lc4j.model.request.ChatRequest;
//import com.ggzed.lc4j.result.Result;
//import jakarta.validation.Valid;
//import org.springframework.web.bind.annotation.*;
//
///**
// * 对外API，路径与入参对齐 yp-assistant-engine
// */
//@RestController
//@RequestMapping("/tool")
//public class McpController {
//
//    private final FinancialAgent financialAgent;
//
//    private final AgentKernel agentKernel;
//
//    public McpController(FinancialAgent financialAgent, AgentKernel agentKernel) {
//        this.financialAgent = financialAgent;
//        this.agentKernel = agentKernel;
//    }
//
//    /**
//     * 挂载新方法
//     * @param request
//     * @return
//     */
//    @PostMapping("/add")
//    public Result<ChatResponse> simple(@RequestBody @Valid ChatRequest request) {
//        String content = financialAgent.chat(request.sessionId(), request.message());
//        return Result.success(new ChatResponse(request.sessionId(), content));
//    }
//}
