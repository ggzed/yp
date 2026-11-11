package com.ggzed.mcpserver.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class BusinessMcpTools {

    private final RestClient restClient;

    public BusinessMcpTools() {
        // 1. 设置超时时间（例如：连接超时 2秒，读取超时 3秒）
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .baseUrl("http://localhost:8082")
                .build();
    }

    @Tool(description = "根据客户号查询客户的风险承受等级。")
    public String queryRiskLevel(@ToolParam(description = "客户号，例如 VIP001") String customerId) {
        try {
            // 发起调用
            String response = restClient.get()
                    .uri("/business/risk?customerId={id}", customerId)
                    .retrieve()
                    .body(String.class);
            return "风控查询结果：" + response;

        } catch (Exception e) {
            // 2. 核心：降级兜底处理
            // 绝对不要把原生的 Exception 抛给大模型，要用自然语言返回
            System.err.println("调用 8081 业务服务失败: " + e.getMessage());
            return "【系统提示】当前风控查询服务正在升级或网络繁忙，暂时无法获取客户 " + customerId + " 的真实风险等级。请提醒用户稍后再试，或转接人工客服。";
        }
    }
}