package com.demo.config;

import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIModelConfig {

    @Bean
    public OpenAiChatModel openAiChatModel(APIConfig apiConfig) {
        return OpenAiChatModel.builder()
                .baseUrl(apiConfig.getBaseUrl())
                .apiKey(apiConfig.getApiKey())
                .modelName(apiConfig.getModel())
                .build();
    }
}