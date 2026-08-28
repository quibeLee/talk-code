package com.talkcode.ai;


import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.talkcode.ai.guardrail.PromptSafetyInputGuardrail;
import com.talkcode.ai.service.AiCodeCreateService;
import com.talkcode.ai.service.AiCodeGeneratorService;
import com.talkcode.ai.service.AiCodeModifyService;
import com.talkcode.ai.tools.ToolManager;
import com.talkcode.model.enums.CodeGenTypeEnum;
import com.talkcode.service.ChatHistoryService;
import com.talkcode.utils.SpringContextUtil;
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
 * <p>
 * 职责：
 * 1. HTML / 多文件（无工具）-> {@link AiCodeGeneratorService}
 * 2. Vue 创建（仅文件写入工具）-> {@link AiCodeCreateService}
 * 3. Vue 修改（读/改/写/删/列目录工具）-> {@link AiCodeModifyService}
 * <p>
 * 核心优化：按场景只传递最小工具集，减少模型可选择的工具面，从而降低工具调用幻觉、提高输出准确度。
 */
@Configuration
@Slf4j
public class AiCodeGeneratorServiceFactory {

    @Resource(name = "openAiChatModel")
    private ChatModel chatModel;

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
     * 文件工具
     */
    @Resource
    private ToolManager toolManager;

    /**
     * HTML / 多文件服务缓存（按 应用ID + 生成类型）
     */
    private final Cache<String, AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> log.info("HTML / 多文件 AI 生成服务实例缓存被移除: cacheKey: {}, 原因: {}", key, cause))
            .build();

    /**
     * Vue 创建服务缓存（按 应用ID + 生成类型）
     */
    private final Cache<String, AiCodeCreateService> vueCreateServiceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> log.info("Vue 创建服务缓存被移除: cacheKey: {}, 原因: {}", key, cause))
            .build();

    /**
     * Vue 修改服务缓存（按 应用ID + 生成类型）
     */
    private final Cache<String, AiCodeModifyService> vueModifyServiceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> log.info("Vue 修改服务缓存被移除: cacheKey: {}, 原因: {}", key, cause))
            .build();

    /**
     * 根据appID获取HTML/多文件AI服务实例（带缓存）
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        String cacheKey = buildCacheKey(appId, codeGenType);
        return serviceCache.get(cacheKey, key -> createAiCodeGeneratorService(appId, codeGenType));
    }

    /**
     * 获取 Vue 创建服务实例（带缓存，key 带 create 场景前缀，与修改服务严格区分）
     */
    public AiCodeCreateService getAiCodeCreateService(long appId, CodeGenTypeEnum codeGenType) {
        String cacheKey = "create_" + buildCacheKey(appId, codeGenType);
        return vueCreateServiceCache.get(cacheKey, key -> createVueCreateService(appId));
    }

    /**
     * 获取 Vue 修改服务实例（带缓存，key 带 modify 场景前缀，与创建服务严格区分）
     */
    public AiCodeModifyService getAiCodeModifyService(long appId, CodeGenTypeEnum codeGenType) {
        String cacheKey = "modify_" + buildCacheKey(appId, codeGenType);
        return vueModifyServiceCache.get(cacheKey, key -> createVueModifyService(appId));
    }


    /**
     * 创建 HTML / 多文件 AI 服务实例（无工具）
     */
    private AiCodeGeneratorService createAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        MessageWindowChatMemory chatMemory = buildChatMemory(appId);
        // 使用多例模式获取StreamingChatModel,解决并发问题
        StreamingChatModel streamingChatModel = SpringContextUtil.getBean("streamingChatModelPrototype", StreamingChatModel.class);
        return AiServices.builder(AiCodeGeneratorService.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .chatMemory(chatMemory)
                .build();
    }

    /**
     * 创建 Vue 创建服务实例：只暴露【文件写入工具】，从零生成项目
     */
    private AiCodeCreateService createVueCreateService(long appId) {
        log.info("为 appId: {} 创建 Vue 创建 AI 服务实例", appId);
        MessageWindowChatMemory chatMemory = buildChatMemory(appId);
        // 使用多例模式获取StreamingChatModel,解决并发问题
        StreamingChatModel reasoningStreamingChatModel = SpringContextUtil.getBean("reasoningStreamingChatModelPrototype", StreamingChatModel.class);
        return AiServices.builder(AiCodeCreateService.class)
                .chatModel(chatModel)
                .streamingChatModel(reasoningStreamingChatModel)
                .chatMemoryProvider(memoryId -> chatMemory)
                // 创建场景只需要写入文件这一个工具，减少工具面、降低幻觉
                .tools(toolManager.getTool("writeFile"))
                .maxToolCallingRoundTrips(30) // 工具允许调用次数设置为30
                // 处理幻觉工具调用
                .hallucinatedToolNameStrategy(toolExecutionRequest ->
                        ToolExecutionResultMessage.from(toolExecutionRequest, "Error: there is no tool with name: " + toolExecutionRequest.name()))
                .inputGuardrails(new PromptSafetyInputGuardrail())// 增加用户输入护轨
                .build();
    }

    /**
     * 创建 Vue 修改服务实例：暴露 读/改/写/删/列目录 工具，基于真实文件做最小化修改
     */
    private AiCodeModifyService createVueModifyService(long appId) {
        log.info("为 appId: {} 创建 Vue 修改 AI 服务实例", appId);
        MessageWindowChatMemory chatMemory = buildChatMemory(appId);
        // 使用多例模式获取StreamingChatModel,解决并发问题
        StreamingChatModel reasoningStreamingChatModel = SpringContextUtil.getBean("reasoningStreamingChatModelPrototype", StreamingChatModel.class);
        return AiServices.builder(AiCodeModifyService.class)
                .chatModel(chatModel)
                .streamingChatModel(reasoningStreamingChatModel)
                .chatMemoryProvider(memoryId -> chatMemory)
                .tools((Object[]) toolManager.getAllTools())
                .maxToolCallingRoundTrips(30) // 工具允许调用次数设置为30
                // 处理幻觉工具调用
                .hallucinatedToolNameStrategy(toolExecutionRequest ->
                        ToolExecutionResultMessage.from(toolExecutionRequest, "Error: there is no tool with name: " + toolExecutionRequest.name()))
                .inputGuardrails(new PromptSafetyInputGuardrail()) // 增加用户输入护轨
                .build();
    }

    /**
     * 构建独立的对话记忆（按 appId 存储到 Redis，并从数据库加载历史）
     */
    private MessageWindowChatMemory buildChatMemory(long appId) {
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(20)
                .build();
        // 从数据库中加载对话历史到记忆中
        int loadedMessages = chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 20);
        return chatMemory;
    }


    /**
     * 创建AI代码生成服务实例, 默认应用ID为0,此方法仅供测试使用
     */
    @Bean
    public AiCodeGeneratorService create() {
        return getAiCodeGeneratorService(0L, CodeGenTypeEnum.HTML);
    }

    /**
     * 构建缓存键Key
     */
    private String buildCacheKey(long appId, CodeGenTypeEnum codeGenType) {
        return appId + "_" + codeGenType.getValue();
    }
}
