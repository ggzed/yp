package com.ggzed.mcpserver.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 给所有 REST 控制器统一加上 yml 中配置的接口前缀。
 * MCP 协议端点由 Spring AI 以函数式路由注册，不受此处影响，仍是 /mcp。
 */
@Configuration
public class ApiPrefixConfig implements WebMvcConfigurer {

    @Value("${api.prefix:}")
    private String prefix;

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        if (StringUtils.hasText(prefix)) {
            configurer.addPathPrefix(prefix, HandlerTypePredicate.forAnnotation(RestController.class));
        }
    }
}
