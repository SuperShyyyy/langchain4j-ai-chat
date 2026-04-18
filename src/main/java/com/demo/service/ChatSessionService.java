package com.demo.service;

import com.demo.dto.request.CreateSessionRequest;
import com.demo.dto.response.ChatSessionDetailResponse;
import com.demo.dto.response.ChatSessionResponse;
import com.demo.entity.ChatSession;
import java.util.List;

public interface ChatSessionService {

    ChatSessionResponse createSession(Long userId, CreateSessionRequest request);

    List<ChatSessionResponse> listSessions(Long userId);

    ChatSessionDetailResponse getSessionDetail(Long userId, Long sessionId);

    void deleteSession(Long userId, Long sessionId);

    ChatSession requireOwnedSession(Long userId, Long sessionId);

    String prepareMemory(Long userId, Long sessionId);

    void appendUserMessage(Long userId, Long sessionId, String content);

    void appendAssistantMessage(Long userId, Long sessionId, String content);
}
