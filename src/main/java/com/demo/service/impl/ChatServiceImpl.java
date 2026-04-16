package com.demo.service.impl;

import com.demo.config.APIConfig;
import com.demo.service.ChatService;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ChatServiceImpl implements ChatService {

    @Autowired
    private OpenAiChatModel model;

    @Override
    public String chat(String msg) {
        return model.chat(msg);
    }
}