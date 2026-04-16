package com.demo.controller;

import com.demo.common.ChatRequest;
import com.demo.common.Result;
import com.demo.service.ChatService;
import dev.langchain4j.data.message.ChatMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
public class ChatController {

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
    }
}
