package com.demo.controller;

import com.demo.common.Result;
import com.demo.context.UserContext;
import com.demo.dto.request.ChatSendRequest;
import com.demo.dto.request.CreateSessionRequest;
import com.demo.dto.response.ChatSessionDetailResponse;
import com.demo.dto.response.ChatSessionResponse;
import com.demo.service.ChatSessionService;
import com.demo.service.ConsultantService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ConsultantService consultantService;
    private final ChatSessionService chatSessionService;

    public ChatController(ConsultantService consultantService, ChatSessionService chatSessionService) {
        this.consultantService = consultantService;
        this.chatSessionService = chatSessionService;
    }

    @PostMapping("/sessions")
    public Result<ChatSessionResponse> createSession(@RequestBody(required = false) CreateSessionRequest request) {
        return Result.success(chatSessionService.createSession(UserContext.requireCurrentUserId(), request));
    }

    @GetMapping("/sessions")
    public Result<List<ChatSessionResponse>> listSessions() {
        return Result.success(chatSessionService.listSessions(UserContext.requireCurrentUserId()));
    }

    @GetMapping("/sessions/{sessionId}")
    public Result<ChatSessionDetailResponse> getSessionDetail(@PathVariable Long sessionId) {
        return Result.success(chatSessionService.getSessionDetail(UserContext.requireCurrentUserId(), sessionId));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public Result<Void> deleteSession(@PathVariable Long sessionId) {
        chatSessionService.deleteSession(UserContext.requireCurrentUserId(), sessionId);
        return Result.success();
    }

    @PostMapping(value = "/sessions/{sessionId}/messages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@PathVariable Long sessionId, @Valid @RequestBody ChatSendRequest request) {
        Long userId = UserContext.requireCurrentUserId();
        String memoryId = chatSessionService.prepareMemory(userId, sessionId);
        chatSessionService.appendUserMessage(userId, sessionId, request.getMessage());
        StringBuilder assistantReply = new StringBuilder();
        return consultantService.chat(memoryId, request.getMessage())
                .doOnNext(assistantReply::append)
                .doOnComplete(() -> chatSessionService.appendAssistantMessage(userId, sessionId, assistantReply.toString()));
    }
}
