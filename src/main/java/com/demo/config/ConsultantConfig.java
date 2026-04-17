package com.demo.config;

import com.demo.repository.RedisChatMemoryStore;
import com.demo.service.ConsultantService;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConsultantConfig {

    @Autowired
    private OpenAiChatModel model;
    @Autowired
    private RedisChatMemoryStore redisMemoryStore;

    @Bean
    public ChatMemory chatMemory() {
      return  MessageWindowChatMemory.builder()
              .maxMessages(20)
              .build();
    }

    @Bean
    public ChatMemoryProvider chatMemoryProvider() {
        return new ChatMemoryProvider() {
            @Override
            public ChatMemory get(Object memoryId) {
                return MessageWindowChatMemory
                        .builder()
                        .id(memoryId)
                        .chatMemoryStore(redisMemoryStore)
                        .maxMessages(20)
                        .build();
            }
        };
    }
}
