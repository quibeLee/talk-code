package com.talkcode.ai;

import com.talkcode.ai.service.AiTitleGenderatorService;
import com.talkcode.utils.SpringContextUtil;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 轻量任务标题生成服务工厂
 * 使用轻量快速模型（qwen-turbo）避免创建应用缓慢。
 */
@Slf4j
@Configuration
public class AiCodeGenTitleServiceFactory {


    /**
     * 创建标题生成 AI 服务实例
     */
    @Bean
    public AiTitleGenderatorService createAiTitleGenderatorService() {
        ChatModel simpleTaskChatModel = SpringContextUtil.getBean("simpleTaskChatModelPrototype", ChatModel.class);
        return AiServices.builder(AiTitleGenderatorService.class)
                .chatModel(simpleTaskChatModel)
                .build();
    }

}
