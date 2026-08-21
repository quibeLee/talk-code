package com.heng.hengaicode;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.heng.hengaicode.mapper")
public class HengAiCodeApplication {

    public static void main(String[] args) {
        SpringApplication.run(HengAiCodeApplication.class, args);
    }
}
