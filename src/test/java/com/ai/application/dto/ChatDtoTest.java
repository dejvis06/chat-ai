package com.ai.application.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import com.ai.domain.entity.Chat;
import com.ai.domain.entity.NoSqlChat;
import com.ai.domain.entity.SqlChat;
import org.junit.jupiter.api.Test;

class ChatDtoTest {

    @Test
    void from_shouldMapFromNoSqlChat() {
        Instant created = Instant.parse("2025-01-01T00:00:00Z");
        NoSqlChat chat = new NoSqlChat("s1", "First", created);
        List<ChatMessageDto> msgs = List.of(new ChatMessageDto("USER", "hi"));

        ChatDto dto = ChatDto.from(chat, msgs);

        assertThat(dto.id()).isEqualTo("s1");
        assertThat(dto.name()).isEqualTo("First");
        assertThat(dto.createdAt()).isEqualTo(created); // uses NoSqlChat.getCreatedAt()
        assertThat(dto.messages()).containsExactlyElementsOf(msgs);
    }

    @Test
    void from_shouldMapFromSqlChat() {
        Instant created = Instant.parse("2025-02-02T12:00:00Z");
        SqlChat chat = new SqlChat("42", "DB Chat", created);
        List<ChatMessageDto> msgs = List.of(new ChatMessageDto("ASSISTANT", "hello"));

        ChatDto dto = ChatDto.from(chat, msgs);

        assertThat(dto.id()).isEqualTo("42");
        assertThat(dto.name()).isEqualTo("DB Chat");
        assertThat(dto.createdAt()).isEqualTo(created); // uses Chat.getCreatedAt()
        assertThat(dto.messages()).containsExactlyElementsOf(msgs);
    }

    @Test
    void from_shouldThrowForUnsupportedChatImplementation() {
        Chat unknown = new Chat() {
            @Override public String getId() { return "x"; }
            @Override public Instant getCreatedAt() { return Instant.now(); }
        };

        assertThatThrownBy(() -> ChatDto.from(unknown, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported Chat implementation");
    }
}
