package com.talkcode.config;

import cn.hutool.core.util.StrUtil;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.community.store.memory.chat.redis.StoreType;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
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
        RedisChatMemoryStore.Builder builder = RedisChatMemoryStore.builder()
                .host(host)
                .port(port)
                .password(password)
                // 解决 Redis缺失JSON.GET 异常问题
                .storeType(StoreType.STRING)
                .ttl(ttl);
        if (StrUtil.isNotBlank(password)) {
            builder.user("default");
        }
        return builder.build();
    }

    /**
     * 业务实际使用的记忆存储：
     * 装饰链 Redis → Sanitizing（清洗非法消息） → Compacting（微压缩旧工具结果）
     */
    @Bean
    public ChatMemoryStore chatMemoryStore(RedisChatMemoryStore redisChatMemoryStore) {
        return new CompactingChatMemoryStore(new SanitizingChatMemoryStore(redisChatMemoryStore));
    }
}


