package com.demo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.chat")
public class AppChatProperties {

    private int memoryMaxMessages = 20;

    private int memoryTtlDays = 7;
}
