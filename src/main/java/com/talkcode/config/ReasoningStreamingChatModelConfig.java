package com.talkcode.config;

import com.talkcode.monitor.AiModelMonitorListener;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.time.Duration;
import java.util.List;

/**
 * 推理流式模型配置类
 */
@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai.reasoning-streaming-chat-model")
@Data
public class ReasoningStreamingChatModelConfig {

    private String baseUrl;
    private String apiKey;
    private String modelName;
    private int maxTokens;
    private Duration timeout;
    private double temperature;
    private boolean logRequests;
    private boolean logResponses;
    @Resource
    private AiModelMonitorListener aiModelMonitorListener;

    /**
     * 推理流式模型（用于 Vue 项目生成，带工具调用）
     */
    @Bean
    @Scope("prototype")
    public StreamingChatModel reasoningStreamingChatModelPrototype() {
        return OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .maxTokens(maxTokens)
                .timeout(timeout)
                .temperature(temperature)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .listeners(List.of(aiModelMonitorListener))
                .build();
    }
}
