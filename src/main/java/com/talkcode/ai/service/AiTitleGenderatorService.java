package com.talkcode.ai.service;

import dev.langchain4j.service.SystemMessage;

/**
 * AI标题生成服务接口
 */
public interface AiTitleGenderatorService {

    /**
     * 根据用户的描述生成标题
     * @param userMessage 用户消息
     * @return 生成的标题
     */
    @SystemMessage(fromResource = "prompt/codegen-title-system-prompt.txt")
    String generateTitle(String userMessage);
}
