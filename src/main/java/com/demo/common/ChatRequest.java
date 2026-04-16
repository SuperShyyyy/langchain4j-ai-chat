package com.demo.common;

import lombok.Data;

@Data
public class ChatRequest {

    private String message;

    private String userId;

    private Boolean stream = true;

    private Boolean enableSearch = true;

    private Double temperature = 0.75;

    private Integer maxTokens = 1024;

    private Double topP = 0.9;

}