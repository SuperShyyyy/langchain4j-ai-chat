package com.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.demo.dto.request.CreateSessionRequest;
import com.demo.dto.response.ChatMessageResponse;
import com.demo.dto.response.ChatSessionDetailResponse;
import com.demo.dto.response.ChatSessionResponse;
import com.demo.entity.ChatMessage;
import com.demo.entity.ChatSession;
import com.demo.exception.NotFoundException;
import com.demo.mapper.ChatMessageMapper;
import com.demo.mapper.ChatSessionMapper;
import com.demo.repository.RedisChatMemoryStore;
import com.demo.service.AuthService;
import com.demo.service.ChatSessionService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatSessionServiceImpl implements ChatSessionService {

    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final RedisChatMemoryStore redisChatMemoryStore;
    private final AuthService authService;

    public ChatSessionServiceImpl(ChatSessionMapper chatSessionMapper,
                                  ChatMessageMapper chatMessageMapper,
                                  RedisChatMemoryStore redisChatMemoryStore,
                                  AuthService authService) {
        this.chatSessionMapper = chatSessionMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.redisChatMemoryStore = redisChatMemoryStore;
        this.authService = authService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatSessionResponse createSession(Long userId, CreateSessionRequest request) {
        authService.requireActiveUser(userId);
        LocalDateTime now = LocalDateTime.now();
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setTitle(resolveTitle(request));
        session.setDeleted(0);
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        session.setLastMessageAt(null);
        chatSessionMapper.insert(session);
        return toSessionResponse(session);
    }

    @Override
    public List<ChatSessionResponse> listSessions(Long userId) {
        authService.requireActiveUser(userId);
        return chatSessionMapper.selectList(new LambdaQueryWrapper<ChatSession>()
                        .eq(ChatSession::getUserId, userId)
                        .eq(ChatSession::getDeleted, 0)
                        .orderByDesc(ChatSession::getUpdatedAt))
                .stream()
                .map(this::toSessionResponse)
                .toList();
    }

    @Override
    public ChatSessionDetailResponse getSessionDetail(Long userId, Long sessionId) {
        ChatSession session = requireOwnedSession(userId, sessionId);
        List<ChatMessageResponse> messages = chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getUserId, userId)
                        .eq(ChatMessage::getSessionId, sessionId)
                        .orderByAsc(ChatMessage::getMessageOrder))
                .stream()
                .map(this::toMessageResponse)
                .toList();
        return ChatSessionDetailResponse.builder()
                .session(toSessionResponse(session))
                .messages(messages)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSession(Long userId, Long sessionId) {
        requireOwnedSession(userId, sessionId);
        LocalDateTime now = LocalDateTime.now();
        chatSessionMapper.update(null, new LambdaUpdateWrapper<ChatSession>()
                .eq(ChatSession::getId, sessionId)
                .eq(ChatSession::getUserId, userId)
                .eq(ChatSession::getDeleted, 0)
                .set(ChatSession::getDeleted, 1)
                .set(ChatSession::getUpdatedAt, now));
        redisChatMemoryStore.deleteMessages(buildMemoryKey(userId, sessionId));
    }

    @Override
    public ChatSession requireOwnedSession(Long userId, Long sessionId) {
        authService.requireActiveUser(userId);
        ChatSession session = chatSessionMapper.selectOne(new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getId, sessionId)
                .eq(ChatSession::getUserId, userId)
                .eq(ChatSession::getDeleted, 0)
                .last("limit 1"));
        if (session == null) {
            throw new NotFoundException("会话不存在或无权访问");
        }
        return session;
    }

    @Override
    public String prepareMemory(Long userId, Long sessionId) {
        requireOwnedSession(userId, sessionId);
        String memoryKey = buildMemoryKey(userId, sessionId);
        if (redisChatMemoryStore.hasMemory(memoryKey)) {
            return memoryKey;
        }

        List<ChatMessage> history = chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getUserId, userId)
                .eq(ChatMessage::getSessionId, sessionId)
                .orderByAsc(ChatMessage::getMessageOrder));
        if (!history.isEmpty()) {
            List<dev.langchain4j.data.message.ChatMessage> seededMessages = new ArrayList<>();
            for (ChatMessage message : history) {
                switch (message.getRole()) {
                    case "assistant" -> seededMessages.add(AiMessage.from(message.getContent()));
                    case "system" -> seededMessages.add(SystemMessage.from(message.getContent()));
                    default -> seededMessages.add(UserMessage.from(message.getContent()));
                }
            }
            redisChatMemoryStore.updateMessages(memoryKey, seededMessages);
        }
        return memoryKey;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void appendUserMessage(Long userId, Long sessionId, String content) {
        ChatSession session = requireOwnedSession(userId, sessionId);
        saveMessage(userId, sessionId, "user", content);
        updateSessionAfterMessage(session, content);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void appendAssistantMessage(Long userId, Long sessionId, String content) {
        if (content == null || content.isBlank()) {
            return;
        }
        ChatSession session = requireOwnedSession(userId, sessionId);
        saveMessage(userId, sessionId, "assistant", content);
        touchSession(session.getId());
    }

    private void saveMessage(Long userId, Long sessionId, String role, String content) {
        ChatMessage message = new ChatMessage();
        message.setUserId(userId);
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setMessageOrder(nextMessageOrder(userId, sessionId));
        message.setCreatedAt(LocalDateTime.now());
        chatMessageMapper.insert(message);
    }

    private int nextMessageOrder(Long userId, Long sessionId) {
        ChatMessage latestMessage = chatMessageMapper.selectOne(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getUserId, userId)
                .eq(ChatMessage::getSessionId, sessionId)
                .orderByDesc(ChatMessage::getMessageOrder)
                .last("limit 1"));
        return latestMessage == null ? 1 : latestMessage.getMessageOrder() + 1;
    }

    private void updateSessionAfterMessage(ChatSession session, String userMessage) {
        LocalDateTime now = LocalDateTime.now();
        String nextTitle = session.getTitle();
        if (isDefaultTitle(nextTitle)) {
            nextTitle = summarizeTitle(userMessage);
        }
        chatSessionMapper.update(null, new LambdaUpdateWrapper<ChatSession>()
                .eq(ChatSession::getId, session.getId())
                .set(ChatSession::getTitle, nextTitle)
                .set(ChatSession::getUpdatedAt, now)
                .set(ChatSession::getLastMessageAt, now));
    }

    private void touchSession(Long sessionId) {
        LocalDateTime now = LocalDateTime.now();
        chatSessionMapper.update(null, new LambdaUpdateWrapper<ChatSession>()
                .eq(ChatSession::getId, sessionId)
                .set(ChatSession::getUpdatedAt, now)
                .set(ChatSession::getLastMessageAt, now));
    }

    private String buildMemoryKey(Long userId, Long sessionId) {
        return redisChatMemoryStore.buildMemoryKey(userId, sessionId);
    }

    private String resolveTitle(CreateSessionRequest request) {
        if (request == null || request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            return "新对话";
        }
        return request.getTitle().trim();
    }

    private boolean isDefaultTitle(String title) {
        return title == null || title.isBlank() || "新对话".equals(title);
    }

    private String summarizeTitle(String content) {
        String normalized = content == null ? "" : content.trim().replaceAll("\\s+", " ");
        if (normalized.isEmpty()) {
            return "新对话";
        }
        return normalized.length() > 20 ? normalized.substring(0, 20) : normalized;
    }

    private ChatSessionResponse toSessionResponse(ChatSession session) {
        return ChatSessionResponse.builder()
                .id(session.getId())
                .title(session.getTitle())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .lastMessageAt(session.getLastMessageAt())
                .build();
    }

    private ChatMessageResponse toMessageResponse(ChatMessage message) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .role(message.getRole())
                .content(message.getContent())
                .messageOrder(message.getMessageOrder())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
