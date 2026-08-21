package com.heng.hengaicode.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

/**
 * Spring MVC Json 配置
 * 适配 Spring Boot 4 + Jackson 3
 */
@Configuration
public class JsonConfig {

    /**
     * 添加 Long 转 JSON 精度丢失的配置
     * JavaScript 中 Long 类型最大安全整数为 2^53-1,
     * Java 的 Long 最大值可能超出此范围,导致前端精度丢失
     */
    @Bean
    public ObjectMapper jacksonObjectMapper() {
        // 创建 Long -> String 序列化模块
        SimpleModule module = new SimpleModule();
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);

        // 使用 Jackson 3 的 JsonMapper.builder() 构建,在构建时注册模块
        return JsonMapper.builder()
                .addModule(module)
                .build();
    }
}