package com.demo;

import com.demo.config.AppChatProperties;
import com.demo.config.AppJwtProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.demo.mapper")
@EnableConfigurationProperties({AppJwtProperties.class, AppChatProperties.class})
@SpringBootApplication
public class LangChain4jDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(LangChain4jDemoApplication.class, args);
    }
}
