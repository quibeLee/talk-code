package com.talkcode.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * 路由/标题等轻量任务模型配置
 * <p>
 * 使用便宜快速的模型（如 qwen-turbo）完成标题生成、代码生成类型路由等小任务，
 * 避免这些小请求打到主代码生成大模型（deepseek-v4-flash）上，导致创建应用缓慢。
 * <p>
 */
@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai.simple-task-chat-model")
@Data
public class SimpleTaskChatModelConfig {

    private String baseUrl;
    private String apiKey;
    private String modelName;
    private int maxTokens;
    private boolean logRequests;
    private boolean logResponses;

    /**
     * 轻量任务 ChatModel（如 qwen-turbo）
     */
    @Bean
    @Scope("prototype")
    public ChatModel simpleTaskChatModelPrototype() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .maxTokens(maxTokens)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .build();
    }
}
