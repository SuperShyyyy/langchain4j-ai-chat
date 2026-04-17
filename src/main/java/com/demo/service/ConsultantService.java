package com.demo.service;

import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;
import reactor.core.publisher.Flux;

@AiService(
        wiringMode = AiServiceWiringMode.AUTOMATIC,
        chatModel = "openAiChatModel",
        streamingChatModel = "openAiStreamChatModel"
)
public interface ConsultantService {
   public Flux<String> chat(String message);
}
