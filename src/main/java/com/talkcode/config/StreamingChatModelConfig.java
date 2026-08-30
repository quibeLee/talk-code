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
import java.util.Map;

/**
 * 流式模型配置类
 */
@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai.streaming-chat-model")
@Data
public class StreamingChatModelConfig {

    private String baseUrl;
    private String apiKey;
    private String modelName;
    private int maxTokens;
    private Duration timeout;
    private boolean logRequests;
    private boolean logResponses;

    @Resource
    private AiModelMonitorListener aiModelMonitorListener;


    /**
     * 流式模型, 用于原生HTML、多文件代码生成
     */
    @Bean
    @Scope("prototype")
    public StreamingChatModel streamingChatModelPrototype() {
        return OpenAiStreamingChatModel.builder()
                // 禁用推理模式，避免模型的reason-content导致tokens超量,问题：“推理吃光预算导致空内容”
                .customParameters(Map.of("thinking", Map.of("type", "disabled")))
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .maxTokens(maxTokens)
                .timeout(timeout)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .listeners(List.of(aiModelMonitorListener))
                .build();
    }
}
