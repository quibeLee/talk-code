package com.talkcode.ai.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

/**
 * AI 代码修改服务（应用修改迭代场景）
 * <p>
 * 负责修改已存在的 Vue 项目。工厂会为其绑定读/改/写/删/列目录工具，
 * 提示词要求基于真实文件内容做最小化修改，而不是重新生成整个项目。
 */
public interface AiCodeModifyService {

    /**
     * 修改已有 Vue 项目（流式，带工具调用）
     *
     * @param appId       应用ID（作为工具的内存ID）
     * @param userMessage 用户消息
     * @return 修改过程的流式响应
     */
    @SystemMessage(fromResource = "prompt/codegen-vue-modify-system-prompt.txt")
    TokenStream modifyVueProjectStream(@MemoryId long appId, @UserMessage String userMessage);
}
