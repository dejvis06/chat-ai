package com.ai.infrastructure.repository;

import com.ai.BaseTest;
import com.ai.domain.entity.NoSqlChat;
import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.cassandra.core.cql.CqlTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RedisMessageWindowChatMemoryTest extends BaseTest {

    @Autowired
    RedisMessageWindowChatMemory<NoSqlChat> chatMemory;

    @Autowired
    CqlTemplate cqlTemplate;

    @Test
    void add_shouldPersistMessages_in_ai_chat_message() {
        String chatId = "s-add-1";

        // given: two messages with timestamps in metadata (repository uses them for PK clustering)
        Instant t1 = Instant.now();
        Instant t2 = Instant.now().plusSeconds(2);

        List<Message> messages = List.of(
                UserMessage.builder().text("Hello").metadata(Map.of("msg_timestamp", t1)).build(),
                new AssistantMessage("Hi back!", Map.of("msg_timestamp", t2))
        );

        // when
        chatMemory.add(chatId, messages);

        // verify
        List<Map<String, Object>> rows = cqlTemplate.queryForList(
                "SELECT session_id, msg_type, msg_content FROM ai_chat_message WHERE session_id = ?",
                chatId
        );

        assertThat(rows).hasSize(2);
        // types present
        assertThat(rows.stream().map(r -> (String) r.get("msg_type")))
                .containsExactlyInAnyOrder("user", "assistant");
        // contents match
        assertThat(rows.stream().filter(r -> "user".equals(r.get("msg_type"))).findFirst().orElseThrow().get("msg_content"))
                .isEqualTo("Hello");
        assertThat(rows.stream().filter(r -> "assistant".equals(r.get("msg_type"))).findFirst().orElseThrow().get("msg_content"))
                .isEqualTo("Hi back!");
    }

    @Test
    void get_shouldReturnLastNMessages() {
        String chatId = "s-get-1";
        long base = System.currentTimeMillis();

        // Insert 5 messages directly via CqlTemplate
        for (int i = 1; i <= 5; i++) {
            cqlTemplate.execute(
                    "INSERT INTO ai_chat_message (session_id, msg_timestamp, msg_type, msg_content) " +
                            "VALUES (?, ?, ?, ?)",
                    chatId,
                    Instant.ofEpochMilli(base + (i * 1000L)),
                    "user",
                    "Message-" + i
            );
        }

        // Act: fetch with memory.get(..) (maxMessages=3 in constructor)
        List<Message> messages = chatMemory.get(chatId);

        // Assert: only the 3 latest messages should be returned (DESC order by msg_timestamp)
        assertThat(messages).hasSize(5);
        assertThat(messages.get(0).getText()).isEqualTo("Message-5");
        assertThat(messages.get(1).getText()).isEqualTo("Message-4");
        assertThat(messages.get(2).getText()).isEqualTo("Message-3");
    }

    @Test
    void deleteById_shouldRemoveMessagesMemoryAndIndexRow() {
        String chatId = "s-del-1";
        String otherId = "s-keep";
        Instant createdAt = Instant.now().minusSeconds(60);
        UUID createdAtTimeUuid = Uuids.startOf(createdAt.toEpochMilli());

        // --- seed target chat ---
        // ai_chat_memory
        cqlTemplate.execute(
                "INSERT INTO ai_chat_memory (session_id, session_name, created_at) VALUES (?, ?, ?)",
                chatId, "Chat A", createdAt
        );
        // chats_by_created (must match createdAt->timeuuid)
        cqlTemplate.execute(
                "INSERT INTO chats_by_created (bucket, created_at, session_id, session_name) VALUES ('all', ?, ?, ?)",
                createdAtTimeUuid, chatId, "Chat A"
        );
        // ai_chat_message (2 rows)
        long base = System.currentTimeMillis();
        cqlTemplate.execute(
                "INSERT INTO ai_chat_message (session_id, msg_timestamp, msg_type, msg_content) VALUES (?, ?, ?, ?)",
                chatId, Instant.ofEpochMilli(base - 1000), "user", "hi"
        );
        cqlTemplate.execute(
                "INSERT INTO ai_chat_message (session_id, msg_timestamp, msg_type, msg_content) VALUES (?, ?, ?, ?)",
                chatId, Instant.ofEpochMilli(base), "assistant", "hello"
        );

        // --- seed another chat to verify it remains ---
        Instant otherCreated = createdAt.plusSeconds(1);
        UUID otherTimeUuid = Uuids.startOf(otherCreated.toEpochMilli());
        cqlTemplate.execute(
                "INSERT INTO ai_chat_memory (session_id, session_name, created_at) VALUES (?, ?, ?)",
                otherId, "Chat B", otherCreated
        );
        cqlTemplate.execute(
                "INSERT INTO chats_by_created (bucket, created_at, session_id, session_name) VALUES ('all', ?, ?, ?)",
                otherTimeUuid, otherId, "Chat B"
        );
        cqlTemplate.execute(
                "INSERT INTO ai_chat_message (session_id, msg_timestamp, msg_type, msg_content) VALUES (?, ?, ?, ?)",
                otherId, Instant.ofEpochMilli(base + 1000), "user", "keep me"
        );

        // --- act ---
        chatMemory.clear(chatId);

        // --- assert: ai_chat_message cleared for target chat ---
        Long msgCount = cqlTemplate.queryForObject(
                "SELECT count(*) FROM ai_chat_message WHERE session_id = ?",
                Long.class, chatId
        );
        assertThat(msgCount).isZero();

        // other chat still has messages
        Long otherMsgCount = cqlTemplate.queryForObject(
                "SELECT count(*) FROM ai_chat_message WHERE session_id = ?",
                Long.class, otherId
        );
        assertThat(otherMsgCount).isEqualTo(1L);

        // ai_chat_memory row deleted
        List<Map<String, Object>> mem = cqlTemplate.queryForList(
                "SELECT session_id FROM ai_chat_memory WHERE session_id = ?",
                chatId
        );
        assertThat(mem).isEmpty();

        // chats_by_created row deleted (check exact primary key)
        List<Map<String, Object>> idx = cqlTemplate.queryForList(
                "SELECT session_id FROM chats_by_created WHERE bucket = 'all' AND created_at = ? AND session_id = ?",
                createdAtTimeUuid, chatId
        );
        assertThat(idx).isEmpty();

        // other chat index row remains
        List<Map<String, Object>> otherIdx = cqlTemplate.queryForList(
                "SELECT session_id FROM chats_by_created WHERE bucket = 'all' AND created_at = ? AND session_id = ?",
                otherTimeUuid, otherId
        );
        assertThat(otherIdx).hasSize(1);
    }
}
