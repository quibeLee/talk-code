package com.talkcode.ai;

import com.talkcode.ai.service.AiCodeGenTypeRoutingService;
import com.talkcode.utils.SpringContextUtil;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 轻量任务代码生成类型路由服务工厂
 * 使用轻量快速模型（qwen-turbo）避免创建应用缓慢。
 */
@Slf4j
@Configuration
public class AiCodeGenTypeRountinhServiceFactory {

    /**
     * 创建AI代码生成类型路由服务实例
     */
    @Bean
    public AiCodeGenTypeRoutingService createAiCodeGenTypeRoutingService() {
        ChatModel simpleTaskChatModel = SpringContextUtil.getBean("simpleTaskChatModelPrototype", ChatModel.class);
        return AiServices.builder(AiCodeGenTypeRoutingService.class)
                .chatModel(simpleTaskChatModel)
                .build();
    }
}
