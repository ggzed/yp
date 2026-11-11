package com.ggzed.lc4j.llm;

import com.ggzed.lc4j.model.dto.ModelConfig;
import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder;
import dev.langchain4j.http.client.spring.restclient.SpringRestClient;
import dev.langchain4j.http.client.spring.restclient.SpringRestClientBuilder;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.prometheus.client.Counter.build;

@Service
public class ModelFactory {
    // 使用线程安全的缓存，Key 可以是 "provider:modelName:apiKey的hash"
    private final Map<String, ChatModel> modelCache = new ConcurrentHashMap<>();
    public ChatModel getOrCreateModel(ModelConfig config) {
        String cacheKey = config.getProvider() + ":" + config.getModelName() + ":" + config.getApiKey().hashCode();
        return modelCache.computeIfAbsent(cacheKey, k -> {
            // 只有缓存中没有时，才会执行这里的构建逻辑
            if ("ollama".equals(config.getProvider())) {
                return createOpenAiModel(config);
            } else {
                return createOpenAiModel(config);
            }
        });

//        switch (config.getProvider().toLowerCase()) {
//            case "openai":
//                return createOpenAiModel(config);
//            case "ollama":
//                return createOpenAiModel(config);
//            case "deepseek":
//                return createOpenAiModel(config);
//            default:
//                throw new IllegalArgumentException("Unsupported provider: " + config.getProvider());
//        }
    }


    private ChatModel createOpenAiModel(ModelConfig request) {
//        return OllamaChatModel.builder()
//                .baseUrl(request.getBaseUrl())
//                .apiKey(request.getApiKey())
//                .modelName(request.getModelName())
//                .build();
        SpringRestClientBuilder springRestClientBuilder = SpringRestClient.builder();
        // 1. 构建模型实例
        return OllamaChatModel.builder()
                .baseUrl("http://192.168.101.108:11434") // 你的 Ollama 地址
                .modelName("qwen2.5:7b")                 // 模型名称
                .temperature(0.7)// 温度参数
                .httpClientBuilder(springRestClientBuilder) // 显式指定
                .timeout(Duration.ofSeconds(120))        // 重点：大模型生成慢，超时时间设大一点
                .build();
    }
}
