package com.talkcode.ai.service;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class AiTitleGenderatorServiceTest {

    @Resource
    private AiTitleGenderatorService aiTitleGenderatorService;

    @Test
    void generateTitle() {
        String title = aiTitleGenderatorService.generateTitle("这是一个测试");
        assertNotNull(title);
    }
}