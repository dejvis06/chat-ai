package com.ai.application.domain.entity;

import com.ai.domain.entity.NoSqlChat;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class NoSqlChatTest {

    @Test
    void ctor_withIdAndCreatedAt_setsAllFields() {
        Instant created = Instant.parse("2025-01-01T00:00:00Z");
        NoSqlChat chat = new NoSqlChat("c1", "My Chat", created);

        assertThat(chat.getId()).isEqualTo("c1");
        assertThat(chat.getName()).isEqualTo("My Chat");
        assertThat(chat.getCreatedAt()).isEqualTo(created);
    }

    @Test
    void ctor_withNameAndId_setsNameAndGeneratesCreatedAt() {
        NoSqlChat chat = new NoSqlChat("My Chat", "c2");

        assertThat(chat.getId()).isEqualTo("c2");
        assertThat(chat.getName()).isEqualTo("My Chat");
        assertThat(chat.getCreatedAt()).isNotNull(); // set by super(id) constructor
    }
}

