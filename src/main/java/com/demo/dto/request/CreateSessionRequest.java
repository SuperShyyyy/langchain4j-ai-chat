package com.demo.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateSessionRequest {

    @Size(max = 128, message = "会话标题长度不能超过 128 位")
    private String title;
}
