package com.ai.application.domain.entity;

import com.ai.domain.entity.SqlChat;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SqlChatTest {

    @Test
    void constructor_withIdNameAndCreatedAt_setsAllFields() {
        Instant createdAt = Instant.now();
        SqlChat chat = new SqlChat("id1", "chatName", createdAt);

        assertThat(chat.getId()).isEqualTo("id1");
        assertThat(chat.getName()).isEqualTo("chatName");
        assertThat(chat.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void constructor_withName_setsNameAndNullId() {
        SqlChat chat = new SqlChat("onlyName");

        assertThat(chat.getName()).isEqualTo("onlyName");
        assertThat(chat.getId()).isNull();
        assertThat(chat.getCreatedAt()).isNotNull(); // createdAt should default to now
    }

    @Test
    void setName_updatesName() {
        SqlChat chat = new SqlChat("initialName");
        chat.setName("updatedName");

        assertThat(chat.getName()).isEqualTo("updatedName");
    }
}
