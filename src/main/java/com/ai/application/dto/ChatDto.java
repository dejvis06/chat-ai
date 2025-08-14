package com.ai.application.dto;

import com.ai.domain.entity.Chat;
import com.ai.domain.entity.NoSqlChat;
import com.ai.domain.entity.SqlChat;

import java.time.Instant;
import java.util.List;

public record ChatDto(
        String id,
        String name,
        Instant createdAt,
        List<ChatMessageDto> messages
) {
    public static ChatDto from(Chat chat, List<ChatMessageDto> messages) {
        if (chat instanceof NoSqlChat noSqlChat) {
            return new ChatDto(noSqlChat.getId(), noSqlChat.getId(), noSqlChat.getCreatedAt(), messages);
        }
        if (chat instanceof SqlChat sqlChat) {
            return new ChatDto(sqlChat.getId(), sqlChat.getName(), chat.getCreatedAt(), messages);
        }
        throw new IllegalArgumentException(
                "Unsupported Chat implementation: " + chat.getClass().getName()
        );
    }
}

