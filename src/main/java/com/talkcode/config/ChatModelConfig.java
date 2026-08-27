package com.talkcode.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * 非流式模型配置类
 */
@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai.chat-model")
@Data
public class ChatModelConfig {

    private String baseUrl;
    private String apiKey;
    private String modelName;
    private int maxTokens;
    private boolean logRequests;
    private boolean logResponses;


    /**
     * 非流式模型, 用于原生HTML、多文件代码生成
     */
    @Bean
    public ChatModel chatModel() {
        return OpenAiChatModel.builder()
                // 禁用推理模式，避免模型的reason-content导致tokens超量,问题：“推理吃光预算导致空内容”
                .customParameters(Map.of("thinking", Map.of("type", "disabled")))
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .maxTokens(maxTokens)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .build();
    }
}
