//package com.ggzed.lc4j.agent;
//
//import dev.langchain4j.service.MemoryId;
//import dev.langchain4j.service.SystemMessage;
//import dev.langchain4j.service.UserMessage;
//import dev.langchain4j.service.spring.AiService;
//import dev.langchain4j.service.spring.AiServiceWiringMode;
//
//@AiService(
//        wiringMode = AiServiceWiringMode.EXPLICIT,
//        chatModel = "localModelChatModel",
//        chatMemoryProvider = "chatMemoryProvider",
//        tools = "financeTools" // 👈 直接声明你要用的工具 Bean 名称
//)
//public interface FinancialAgent {
//
//    @SystemMessage("你是一个专业的银行理财经理。回答要简明扼要，严禁编造。")
//    String chat(@MemoryId String sessionId, @UserMessage String message);
//}