package com.ggzed.lc4j.config;

import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 连接 yp-agent-mcp-hub：优先从 Nacos 取实例地址，未注册时回退到本地开发地址。
 */
@Slf4j
@Configuration
public class McpConfig {

    private final DiscoveryClient discoveryClient;

    @Value("${mcp.client.service-name:yp-agent-mcp-hub}")
    private String serviceName;

    @Value("${mcp.client.endpoint:/mcp}")
    private String endpoint;

    @Value("${mcp.client.fallback-url:http://localhost:8083}")
    private String fallbackUrl;

    public McpConfig(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    @Bean
    public McpClient mcpClient() {
        String url = resolveBaseUrl() + endpoint;
        log.info("连接 MCP 服务: {}", url);

        McpTransport transport = new StreamableHttpMcpTransport.Builder()
                .url(url)
                .logRequests(true)
                .logResponses(true)
                .build();

        return new DefaultMcpClient.Builder()
                .transport(transport)
                .build();
    }

    /** Nacos 缓存可能尚未就绪，做有限次重试；始终不可用时回退到本地地址 */
    private String resolveBaseUrl() {
        for (int i = 0; i < 3; i++) {
            try {
                List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
                if (!instances.isEmpty()) {
                    ServiceInstance instance = instances.get(0);
                    return "http://" + instance.getHost() + ":" + instance.getPort();
                }
            } catch (Exception e) {
                log.warn("Nacos 查询 {} 失败: {}", serviceName, e.getMessage());
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        log.warn("Nacos 中未发现 {}，回退到 {}", serviceName, fallbackUrl);
        return fallbackUrl;
    }
}