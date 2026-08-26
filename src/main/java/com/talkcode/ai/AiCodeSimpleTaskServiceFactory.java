package com.talkcode.ai;

import com.talkcode.ai.service.AiCodeGenTypeRoutingService;
import com.talkcode.ai.service.AiTitleGenderatorService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 轻量任务（标题生成、代码生成类型路由等）服务工厂
 * 使用轻量快速模型（qwen-turbo）避免创建应用缓慢。
 */
@Slf4j
@Configuration
public class AiCodeSimpleTaskServiceFactory {

    @Resource
    private ChatModel simpleTaskChatModel;

    /**
     * 创建标题生成 AI 服务实例
     */
    @Bean
    public AiTitleGenderatorService getAiTitleGenderatorService() {
        return AiServices.builder(AiTitleGenderatorService.class)
                .chatModel(simpleTaskChatModel)
                .build();
    }

    /**
     * 创建AI代码生成类型路由服务实例
     */
    @Bean
    public AiCodeGenTypeRoutingService createAiCodeGenTypeRoutingService() {
        return AiServices.builder(AiCodeGenTypeRoutingService.class)
                .chatModel(simpleTaskChatModel)
                .build();
    }
}
