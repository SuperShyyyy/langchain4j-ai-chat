package com.demo.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatMessageResponse {

    private Long id;

    private String role;

    private String content;

    private Integer messageOrder;

    private LocalDateTime createdAt;
}
