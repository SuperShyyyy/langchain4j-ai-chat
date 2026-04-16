package com.demo.common;

import lombok.Data;

@Data
public class ChatRequest {
    private String message;
    private String userId; // 为后面多轮对话准备
}