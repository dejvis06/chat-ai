package com.ai.application.domain.entity;

import com.ai.domain.entity.Chat;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ChatTest {

    static class DummyChat extends Chat {
        DummyChat(String id) {
            super(id);
        }
        DummyChat(String id, Instant createdAt) {
            super(id, createdAt);
        }
    }

    @Test
    void shouldSetIdAndCreatedAtInConstructor() {
        Instant now = Instant.now();
        DummyChat chat = new DummyChat("c1", now);

        assertThat(chat.getId()).isEqualTo("c1");
        assertThat(chat.getCreatedAt()).isEqualTo(now);
    }

    @Test
    void shouldSetCreatedAtAutomaticallyWhenOnlyIdConstructorUsed() {
        DummyChat chat = new DummyChat("c2");

        assertThat(chat.getId()).isEqualTo("c2");
        assertThat(chat.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldAllowIdAndCreatedAtMutation() {
        DummyChat chat = new DummyChat("c3");

        chat.setId("newId");
        Instant later = Instant.now();
        chat.setCreatedAt(later);

        assertThat(chat.getId()).isEqualTo("newId");
        assertThat(chat.getCreatedAt()).isEqualTo(later);
    }
}

