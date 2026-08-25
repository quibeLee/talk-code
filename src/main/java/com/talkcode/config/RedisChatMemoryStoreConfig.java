package com.talkcode.config;

import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.community.store.memory.chat.redis.StoreType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redis 聊天记忆存储配置类
 */


@Configuration
@ConfigurationProperties(prefix = "spring.data.redis")
@Data
public class RedisChatMemoryStoreConfig {
    /**
     * Redis 主机
     */
    private String host;
    /**
     * Redis 端口
     */
    private int port;
    /**
     * Redis 密码
     */
    private String password;
    /**
     * Redis 过期时间
     */
    private long ttl;

    @Bean
    public RedisChatMemoryStore redisChatMemoryStore() {
        return RedisChatMemoryStore.builder()
                .host(host)
                .port(port)
                .password(password)
                .storeType(StoreType.STRING)
                .ttl(ttl)
                .build();
    }
}
