package com.ai.application.dto;

import org.springframework.ai.chat.messages.*;

public record ChatMessageDto(String role, String content) {
    public static ChatMessageDto from(Message message) {
        String role = switch (message) {
            case UserMessage ignored -> "user";
            case AssistantMessage ignored -> "assistant";
            case SystemMessage ignored -> "system";
            default -> "unknown";
        };
        return new ChatMessageDto(role, message.getText());
    }
}

