package com.heng.hengaicode.ai;


import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class AiCodeGeneratorServiceFactory {

    /**
     * 非流式模型
     */
    @Resource
    private ChatModel chatModel;

    /**
     * 流式模型
     */
    @Resource
    private StreamingChatModel streamingChatModel;

    @Bean
    public AiCodeGeneratorService create() {
        return AiServices.builder(AiCodeGeneratorService.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .build();
    }
}
