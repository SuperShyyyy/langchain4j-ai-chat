package com.demo.controller;

import com.demo.common.ChatRequest;
import com.demo.service.ConsultantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@Slf4j
@RestController
@RequestMapping("/ai")
public class ChatController {

    @Autowired
    private ConsultantService consultantService;

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestBody ChatRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求体为空");
        }
        String messageContent = request.getMessage();
        if (messageContent == null || messageContent.trim().isEmpty()) {
            throw new IllegalArgumentException("消息内容不能为空 (message is required)");
        }
        Object memoryId = request.getMemoryId();
        if (memoryId == null || memoryId.toString().trim().isEmpty()) {
            memoryId = request.getUserId() != null ? request.getUserId() : "default-session-" + System.currentTimeMillis();
        }
        log.info("Received request: {}", request);
        return consultantService.chat(memoryId, messageContent);
    }
}
