package com.talkcode.ai;


import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.talkcode.ai.tools.FileWriteTool;
import com.talkcode.exception.BusinessException;
import com.talkcode.exception.ErrorCode;
import com.talkcode.model.enums.CodeGenTypeEnum;
import com.talkcode.service.ChatHistoryService;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * AI 代码生成服务工厂类
 * AiCodeGeneratorService获取流程：
 * 1. 从本地缓存中获取AI服务实例，如果不存在则创建新的 AI 服务实例
 * 2. 根据应用ID创建独立的对话记忆,将对话记忆存储到Redis中
 * 3. 初始化AI服务实例
 */
@Configuration
@Slf4j
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
    private StreamingChatModel openAiStreamingChatModel;

    /**
     * 推理模型
     */
    @Resource
    private StreamingChatModel reasoningStreamingChatModel;

    /**
     * 对话记忆存储, 用于存储每个应用的对话历史
     */
    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;


    /**
     * 对话历史服务
     */
    @Resource
    private ChatHistoryService chatHistoryService;

    /**
     * 缓存 AI 服务实例，避免重复创建
     * 缓存策略：
     * - 最大缓存1000个应用ID的AI服务实例
     * - 写入后 30分钟过期
     * - 访问后 10分钟过期
     * - 缓存移除监听器：记录移除原因
     */
    private final Cache<String, AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.info("AI 服务实例缓存被移除: cacheKey: {}, 原因: {}", key, cause);
            })
            .build();


    /**
     * 根据appID获取AI服务实例（兼容旧逻辑）
     *
     * @param appId 应用ID
     * @return AI 服务实例
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId) {
        return getAiCodeGeneratorService(appId, CodeGenTypeEnum.HTML);
    }

    /**
     * 根据appID获取AI服务实例（带缓存）
     *
     * @param appId 应用ID
     * @return AI 服务实例
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        String cacheKey = buildCacheKey(appId, codeGenType);
        return serviceCache.get(cacheKey, key -> createAiCodeGeneratorService(appId, codeGenType));
    }

    /**
     * 创建新的 AI 服务实例，用于生成代码
     *
     * @param appId 应用ID
     * @return AI 服务实例
     */

    private AiCodeGeneratorService createAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        log.info("为 appId: {} 创建新的 AI 服务实例", appId);
        // 根据 appId 构建独立的对话记忆,将对话记忆存储到Redis中
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(20)
                .build();
        // 从数据库中加载对话历史到记忆中
        chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 20);
        // 根据代码生成类型不同,选择不同的模型
        return switch (codeGenType) {
            // HTML和对文件生成使用普通的聊天模型
            case HTML, MULTI_FILE ->
                AiServices.builder(AiCodeGeneratorService.class)
                        .chatModel(chatModel)
                        .streamingChatModel(openAiStreamingChatModel)
                        .chatMemory(chatMemory)
                        .build();
            // Vue项目使用推理模型
            case VUE_PROJECT ->
                AiServices.builder(AiCodeGeneratorService.class)
                        .chatModel(chatModel)
                        .streamingChatModel(reasoningStreamingChatModel)
                        // 处理对话记忆
                        .chatMemoryProvider(memoryId -> chatMemory)
                        // 添加文件写入工具
                        .tools(new FileWriteTool())
                        // 处理幻觉工具调用
                        .hallucinatedToolNameStrategy(toolExecutionRequest ->
                                ToolExecutionResultMessage.from(toolExecutionRequest, "Error: there is no tool with name: " + toolExecutionRequest.name()))
                        .build();
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型: " + codeGenType);
        };
    }


    /**
     * 创建AI代码生成服务实例, 默认应用ID为0,此方法仅供测试使用
     *
     * @return AI 服务实例
     */
    @Bean
    public AiCodeGeneratorService create() {
        return getAiCodeGeneratorService(0L);
    }

    /**
     * 构建缓存键Key
     * @param appId 应用ID
     * @param codeGenType 代码生成类型
     * @return 缓存键Key
     */
    private String buildCacheKey(long appId, CodeGenTypeEnum codeGenType) {
        return appId + "_" + codeGenType.getValue();
    }
}
