package com.talkcode.ai.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

/**
 * AI 代码创建服务（应用首次生成场景）
 * <p>
 * 只负责从零创建 Vue 项目，工厂会为其绑定最小工具集（仅【文件写入工具】），
 * 避免模型拿到多余工具而产生工具调用幻觉。
 */
public interface AiCodeCreateService {

    /**
     * 从零生成 Vue 项目（流式，带工具调用）
     *
     * @param appId       应用ID（作为工具的内存ID）
     * @param userMessage 用户消息
     * @return 生成过程的流式响应
     */
    @SystemMessage(fromResource = "prompt/codegen-vue-create-system-prompt.txt")
    TokenStream generateVueProjectStream(@MemoryId long appId, @UserMessage String userMessage);
}
