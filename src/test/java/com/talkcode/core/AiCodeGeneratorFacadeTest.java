package com.talkcode.core;

import com.talkcode.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.List;

@SpringBootTest
class AiCodeGeneratorFacadeTest {

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Test
    void generateCode() {
        File file = aiCodeGeneratorFacade.generateCode("帮我创建登录网页，不超过10行", CodeGenTypeEnum.HTML, 1L);
        Assertions.assertNotNull(file);
    }

    @Test
    void generateAndSaveCodeStream() {
        Flux<String> codeStream = aiCodeGeneratorFacade.generateAndSaveCodeStream("帮我创建一个简单登录网页,不超过50行", CodeGenTypeEnum.HTML, 1L);
        // 阻塞等待所有完成
        List<String> codeList = codeStream.collectList().block();
        Assertions.assertNotNull(codeList);
    }
}