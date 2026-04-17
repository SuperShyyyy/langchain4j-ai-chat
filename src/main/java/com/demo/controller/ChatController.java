package com.demo.controller;

import com.demo.common.ChatRequest;
import com.demo.common.Result;
import com.demo.service.ConsultantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import org.springframework.http.MediaType; // 引入 MediaType
@RestController
@RequestMapping("/ai")
public class ChatController {
/*
    @Autowired
    private ChatService chatService;

    @PostMapping("/chat")
    public Result<String> chat(@RequestBody ChatRequest request) {
        try {
            String result = chatService.chat(request.getMessage());
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("AI服务异常");
        }
    }*/

    @Autowired
    private ConsultantService consultantService;

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestBody ChatRequest request) {
        try {
            return consultantService.chat(request.getMessage());
        } catch (Exception e) {
            return Flux.error(new RuntimeException("AI服务异常：" + e.getMessage()));
        }
    }
}
