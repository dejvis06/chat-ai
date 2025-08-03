package com.ai.application.dto;

import com.ai.domain.entity.Chat;

import java.time.Instant;
import java.util.List;

public record ChatDto(
        String id,
        String name,
        Instant createdAt,
        List<ChatMessageDto> messages
) {
    public static ChatDto from(Chat chat, List<ChatMessageDto> messages) {
        return new ChatDto(chat.getId(), chat.getName(), chat.getCreatedAt(), messages);
    }
}

