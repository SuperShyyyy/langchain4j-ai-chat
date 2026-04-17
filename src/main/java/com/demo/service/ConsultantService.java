package com.demo.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;
import reactor.core.publisher.Flux;

@AiService(
        wiringMode = AiServiceWiringMode.AUTOMATIC,
        chatModel = "openAiChatModel",
        streamingChatModel = "openAiStreamChatModel",
        chatMemoryProvider ="chatMemoryProvider"
)
public interface ConsultantService {

    @SystemMessage("你好")
    Flux<String> chat(@MemoryId Object memoryId, @UserMessage String message);
}
