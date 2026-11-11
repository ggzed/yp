//package com.ggzed.lc4j.tool;
//
//import dev.langchain4j.agent.tool.Tool;
//import dev.langchain4j.agent.tool.P;
//
//public class FinanceTools {
//
//    @Tool("计算理财收益")
//    public double calculateProfit(
//            @P("本金") double principal,
//            @P("年化收益率，如0.027") double rate,
//            @P("天数") int days) {
//        return principal * rate * (days / 365.0);
//    }
//}