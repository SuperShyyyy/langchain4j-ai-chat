package com.demo.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.demo.dto.request.CreateSessionRequest;
import com.demo.dto.response.ChatSessionResponse;
import com.demo.entity.ChatMessage;
import com.demo.entity.ChatSession;
import com.demo.exception.NotFoundException;
import com.demo.mapper.ChatMessageMapper;
import com.demo.mapper.ChatSessionMapper;
import com.demo.repository.RedisChatMemoryStore;
import com.demo.service.AuthService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatSessionServiceImplTest {

    @Mock
    private ChatSessionMapper chatSessionMapper;

    @Mock
    private ChatMessageMapper chatMessageMapper;

    @Mock
    private RedisChatMemoryStore redisChatMemoryStore;

    @Mock
    private AuthService authService;

    @InjectMocks
    private ChatSessionServiceImpl chatSessionService;

    private ChatSession session;

    @BeforeEach
    void setUp() {
        session = new ChatSession();
        session.setId(10L);
        session.setUserId(1L);
        session.setTitle("新对话");
        session.setDeleted(0);
    }

    @Test
    void createSessionShouldPersistOwnedSession() {
        CreateSessionRequest request = new CreateSessionRequest();
        request.setTitle("我的会话");
        when(chatSessionMapper.insert(any(ChatSession.class))).thenAnswer(invocation -> {
            ChatSession inserted = invocation.getArgument(0);
            inserted.setId(99L);
            return 1;
        });

        ChatSessionResponse response = chatSessionService.createSession(1L, request);

        ArgumentCaptor<ChatSession> captor = ArgumentCaptor.forClass(ChatSession.class);
        verify(chatSessionMapper).insert(captor.capture());
        assertEquals(1L, captor.getValue().getUserId());
        assertEquals("我的会话", response.getTitle());
        assertEquals(99L, response.getId());
    }

    @Test
    void requireOwnedSessionShouldRejectForeignSession() {
        when(chatSessionMapper.selectOne(any())).thenReturn(null);

        assertThrows(NotFoundException.class, () -> chatSessionService.requireOwnedSession(1L, 99L));
    }

    @Test
    void prepareMemoryShouldSeedRedisFromDatabaseHistory() {
        when(chatSessionMapper.selectOne(any())).thenReturn(session);
        when(redisChatMemoryStore.buildMemoryKey(1L, 10L)).thenReturn("chat:user:1:session:10");
        when(redisChatMemoryStore.hasMemory("chat:user:1:session:10")).thenReturn(false);

        ChatMessage userMessage = new ChatMessage();
        userMessage.setRole("user");
        userMessage.setContent("你好");
        ChatMessage assistantMessage = new ChatMessage();
        assistantMessage.setRole("assistant");
        assistantMessage.setContent("你好，请问有什么可以帮你");
        when(chatMessageMapper.selectList(any())).thenReturn(List.of(userMessage, assistantMessage));

        String memoryKey = chatSessionService.prepareMemory(1L, 10L);

        assertEquals("chat:user:1:session:10", memoryKey);
        verify(redisChatMemoryStore).updateMessages(any(), any());
    }

    @Test
    void appendAssistantMessageShouldSkipBlankContent() {
        chatSessionService.appendAssistantMessage(1L, 10L, " ");

        verify(chatSessionMapper, never()).selectOne(any());
        verify(chatMessageMapper, never()).insert(any(ChatMessage.class));
    }

    @Test
    void getSessionDetailShouldReturnMessages() {
        when(chatSessionMapper.selectOne(any())).thenReturn(session);
        ChatMessage message = new ChatMessage();
        message.setId(1L);
        message.setRole("user");
        message.setContent("hello");
        message.setMessageOrder(1);
        when(chatMessageMapper.selectList(any())).thenReturn(List.of(message));

        var detail = chatSessionService.getSessionDetail(1L, 10L);

        assertNotNull(detail);
        assertEquals(1, detail.getMessages().size());
        assertEquals("hello", detail.getMessages().get(0).getContent());
    }
}
