package com.demo.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatSessionDetailResponse {

    private ChatSessionResponse session;

    private List<ChatMessageResponse> messages;
}
