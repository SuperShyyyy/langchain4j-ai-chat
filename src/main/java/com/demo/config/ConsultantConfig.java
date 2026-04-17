package com.demo.config;

import com.demo.repository.RedisChatMemoryStore;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConsultantConfig {

    private final RedisChatMemoryStore redisMemoryStore;
    private final AppChatProperties appChatProperties;

    public ConsultantConfig(RedisChatMemoryStore redisMemoryStore, AppChatProperties appChatProperties) {
        this.redisMemoryStore = redisMemoryStore;
        this.appChatProperties = appChatProperties;
    }

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(appChatProperties.getMemoryMaxMessages())
                .build();
    }

    @Bean
    public ChatMemoryProvider chatMemoryProvider() {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .chatMemoryStore(redisMemoryStore)
                .maxMessages(appChatProperties.getMemoryMaxMessages())
                .build();
    }
}
