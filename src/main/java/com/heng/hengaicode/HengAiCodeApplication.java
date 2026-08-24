package com.heng.hengaicode;

import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {RedisEmbeddingStoreAutoConfiguration.class})
@MapperScan("com.heng.hengaicode.mapper")
public class HengAiCodeApplication {

    public static void main(String[] args) {
        SpringApplication.run(HengAiCodeApplication.class, args);
    }
}
