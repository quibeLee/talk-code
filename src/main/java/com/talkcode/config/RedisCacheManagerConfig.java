package com.talkcode.config;

import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@Data
public class RedisCacheManagerConfig {

    private final String CACHE_NAME_GOOD_APP_PAGE = "good_app_page";

    @Resource
    private RedisConnectionFactory redisConnectionFactory;

    @Bean
    public CacheManager cacheManager() {

        RedisCacheConfiguration cacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(cacheConfiguration)
                .withCacheConfiguration(CACHE_NAME_GOOD_APP_PAGE, cacheConfiguration.entryTtl(Duration.ofMinutes(5)))
                .build();
    }

}
