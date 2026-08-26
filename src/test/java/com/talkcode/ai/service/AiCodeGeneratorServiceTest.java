package com.talkcode.ai.service;

import com.talkcode.ai.model.HtmlCodeResult;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;

@SpringBootTest
class AiCodeGeneratorServiceTest {

    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;

    @Test
    void generateHtmlCode() {
        HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode("帮我创建一个张三的个人简历，不超过30行");
        Assertions.assertNotNull(result);
    }

    @Test
    void generateMultiFileCode() {
        Flux<String> result = aiCodeGeneratorService
                .generateMultiFileCodeStream("帮我创建一个张三的个人简历");

        List<String> chunks = result.collectList()
                .block(Duration.ofSeconds(60));  // 大模型可能较慢，设超时

        Assertions.assertNotNull(chunks);
        Assertions.assertFalse(chunks.isEmpty(), "流应返回至少一个chunk");

        String fullOutput = String.join("", chunks);
        Assertions.assertFalse(fullOutput.isBlank(), "生成结果不应为空");
    }

    @Test
    void testChatMemory() {
        HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode("做个程序员张三的工具网站，总代码量不超过 20 行");
        Assertions.assertNotNull(result);
        result = aiCodeGeneratorService.generateHtmlCode("不要生成网站，告诉我你刚刚做了什么？");
        Assertions.assertNotNull(result);
        result = aiCodeGeneratorService.generateHtmlCode("做个程序员张三的工具网站，总代码量不超过 20 行");
        Assertions.assertNotNull(result);
        result = aiCodeGeneratorService.generateHtmlCode("不要生成网站，告诉我你刚刚做了什么？");
        Assertions.assertNotNull(result);
    }

}