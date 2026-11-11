package com.ggzed.lc4j.config;

import dev.langchain4j.model.chat.ChatModel;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ModelRegistry {
    private final Map<String, ChatModel> models = new ConcurrentHashMap<>();

    public void register(String name, ChatModel model) {
        models.put(name, model);
    }

    public ChatModel get(String name) {
        ChatModel model = models.get(name);
        if (model == null) {
            throw new IllegalArgumentException("Unknown model: " + name);
        }
        return model;
    }
}
