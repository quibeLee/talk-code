package com.heng.hengaicode.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Redis Session 配置显式,不依赖Spring的默认配置
 * 使用 Redis 作为 HttpSession 的存储实现 ，登录会话存储在 Redis 中，过期时间为 30 天
 */
@Configuration
@EnableRedisHttpSession
public class RedisSessionConfig {
}