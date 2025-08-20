package com.ai.infrastructure.repository;

import com.ai.BaseTest;
import com.ai.domain.entity.NoSqlChat;
import com.ai.domain.model.pagination.ChatPage;
import com.ai.domain.model.pagination.CursorMeta;
import com.ai.domain.model.pagination.OffsetMeta;
import com.ai.domain.model.pagination.PageMeta;
import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CassandraChatRepositoryTest extends BaseTest {

    @Autowired
    CassandraChatMemoryRepository chatRepository;

    @Test
    void saveChat_shouldPersistAndBeRetrievable() {
        // given
        String chatName = "integration-test-chat";

        // when
        NoSqlChat saved = chatRepository.save(chatName);

        // then
        assertThat(saved).isNotNull();
        assertThat(saved.getName()).isEqualTo(chatName);
        assertThat(saved.getId()).isNotBlank();

        // verify it exists in ai_chat_memory
        var dbChat = cqlTemplate.queryForObject(
                "SELECT session_id, session_name, created_at FROM ai_chat_memory WHERE session_id = ?",
                (row, rowNum) -> Map.of(
                        "id", row.getString("session_id"),
                        "name", row.getString("session_name"),
                        "created_at", row.getInstant("created_at")
                ),
                saved.getId()
        );
        assertThat(dbChat.get("id")).isEqualTo(saved.getId());
        assertThat(dbChat.get("name")).isEqualTo(saved.getName());

        Instant expectedInstant = saved.getCreatedAt().truncatedTo(ChronoUnit.MILLIS);
        assertThat(dbChat.get("created_at")).isEqualTo(expectedInstant);

        // verify it exists in chats_by_created
        UUID createdAtTimeUuid = Uuids.startOf(saved.getCreatedAt().toEpochMilli());
        var createdChatsByCreatedEntity = cqlTemplate.queryForObject(
                "SELECT session_id, session_name, created_at " +
                        "FROM chats_by_created " +
                        "WHERE bucket = 'all' AND created_at = ? AND session_id = ?",
                (row, rowNum) -> Map.of(
                        "id", row.getString("session_id"),
                        "name", row.getString("session_name"),
                        "created_at", row.getUuid("created_at")
                ),
                createdAtTimeUuid,
                saved.getId()
        );
        assertThat(createdChatsByCreatedEntity.get("id")).isEqualTo(saved.getId());
        assertThat(createdChatsByCreatedEntity.get("name")).isEqualTo(saved.getName());
        assertThat(createdChatsByCreatedEntity.get("created_at")).isEqualTo(
                Uuids.startOf(expectedInstant.toEpochMilli())
        );
    }

    @Test
    void findByConversationId_returnsMessagesOrderedAndMapped() {
        // given: create a chat
        String chatName = "integration-test-chat";
        NoSqlChat saved = chatRepository.save(chatName);

        // and: insert two messages (user then assistant) with controlled timestamps
        Instant t1 = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        Instant t2 = t1.plusMillis(50);

        cqlTemplate.execute(
                "INSERT INTO ai_chat_message (session_id, msg_timestamp, msg_type, msg_content) VALUES (?, ?, ?, ?)",
                saved.getId(), t1, "user", "hi"
        );
        cqlTemplate.execute(
                "INSERT INTO ai_chat_message (session_id, msg_timestamp, msg_type, msg_content) VALUES (?, ?, ?, ?)",
                saved.getId(), t2, "assistant", "hello"
        );

        // when
        List<Message> messages = chatRepository.findByConversationId(saved.getId());

        // then
        assertThat(messages).hasSize(2);
        assertThat(messages.get(0)).isInstanceOf(AssistantMessage.class);
        assertThat(messages.get(0).getText()).isEqualTo("hello");

        assertThat(messages.get(1)).isInstanceOf(UserMessage.class);
        assertThat(messages.get(1).getText()).isEqualTo("hi");
    }

    @Test
    void findByConversationId_rejectsBlankId() {
        assertThatThrownBy(() -> chatRepository.findByConversationId(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or empty");
    }

    @Test
    void findByConversationId_rejectsNullId() {
        assertThatThrownBy(() -> chatRepository.findByConversationId(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or empty");
    }

    @Test
    void findLastNByConversationId_returnsLatestN_descByTimestamp() {
        // given: a chat
        NoSqlChat saved = chatRepository.save("integration-test-chat");

        // and: three messages with controlled timestamps
        Instant t1 = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        Instant t2 = t1.plusMillis(50);
        Instant t3 = t2.plusMillis(55);

        cqlTemplate.execute(
                "INSERT INTO ai_chat_message (session_id, msg_timestamp, msg_type, msg_content) VALUES (?, ?, ?, ?)",
                saved.getId(), t1, "user", "m1"
        );
        cqlTemplate.execute(
                "INSERT INTO ai_chat_message (session_id, msg_timestamp, msg_type, msg_content) VALUES (?, ?, ?, ?)",
                saved.getId(), t2, "assistant", "m2"
        );
        cqlTemplate.execute(
                "INSERT INTO ai_chat_message (session_id, msg_timestamp, msg_type, msg_content) VALUES (?, ?, ?, ?)",
                saved.getId(), t3, "assistant", "m3"
        );

        // when: fetch last 2 (latest first due to DESC clustering)
        List<Message> lastTwo = chatRepository.findLastNByConversationId(saved.getId(), 2);

        // then: should be t3, t2
        assertThat(lastTwo).hasSize(2);

        assertThat(lastTwo.get(0).getText()).isEqualTo("m3");
        assertThat(lastTwo.get(1).getText()).isEqualTo("m2");
    }

    @Test
    void findLastNByConversationId_rejectsBlankId() {
        assertThatThrownBy(() -> chatRepository.findLastNByConversationId(" ", 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or empty");
    }

    @Test
    void findLastNByConversationId_rejectsNullId() {
        assertThatThrownBy(() -> chatRepository.findLastNByConversationId(null, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or empty");
    }

    @Test
    void deleteById_removesChatAndItsMessages() {
        // given: a chat
        NoSqlChat saved = chatRepository.save("chat-to-delete");

        // and: a message in that chat
        Instant t1 = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        cqlTemplate.execute(
                "INSERT INTO ai_chat_message (session_id, msg_timestamp, msg_type, msg_content) VALUES (?, ?, ?, ?)",
                saved.getId(), t1, "user", "hello"
        );

        // when: delete the chat (should also remove its messages)
        chatRepository.deleteById(saved.getId());

        // then: messages gone
        List<Message> messages = chatRepository.findByConversationId(saved.getId());
        assertThat(messages).isEmpty();

        // and: chat removed from chats_by_created
        List<NoSqlChat> chats = chatRepository.findAll();
        assertThat(chats).isEmpty();

        List<String> chatIds = chatRepository.findConversationIds();
        assertThat(chatIds).isEmpty();
    }

    @Test
    void saveAll_persistsMessages_andFindByConversationIdReturnsThem() {
        // given: a chat
        NoSqlChat saved = chatRepository.save("chat-for-saveAll");

        Instant userMsgInstant = Instant.now();
        Instant assistantMsgInstant = userMsgInstant.plusMillis(50);

        // and: two messages to persist
        List<Message> toSave = List.of(
                UserMessage.builder()
                        .text("hi")
                        .metadata(Map.of("msg_timestamp", userMsgInstant)
                        ).build(),
                new AssistantMessage("hello", Map.of("msg_timestamp", assistantMsgInstant))
        );

        // when
        chatRepository.saveAll(saved.getId(), toSave);

        // then: verify via repository read
        List<Message> stored = chatRepository.findByConversationId(saved.getId());
        assertThat(stored).hasSize(2);

        assertThat(stored.get(0)).isInstanceOf(AssistantMessage.class);
        assertThat(stored.get(0).getText()).isEqualTo("hello");

        assertThat(stored.get(1)).isInstanceOf(UserMessage.class);
        assertThat(stored.get(1).getText()).isEqualTo("hi");
    }

    @Test
    void saveAll_rejectsBlankChatId() {
        assertThatThrownBy(() -> chatRepository.saveAll(" ", List.of(new UserMessage("x"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or empty");
    }

    @Test
    void saveAll_rejectsNullChatId() {
        assertThatThrownBy(() -> chatRepository.saveAll(" ", List.of(new UserMessage("x"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or empty");
    }

    @Test
    void saveAll_rejectsEmptyMessages() {
        NoSqlChat saved = chatRepository.save("chat-for-empty");
        assertThatThrownBy(() -> chatRepository.saveAll(saved.getId(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or empty");
    }

    @Test
    void shouldReturnAllChatsOrderedByCreatedAt() {
        UUID createdAt1 = Uuids.timeBased();

        // second UUID a bit later (e.g. +1 sec)
        UUID createdAt2 = Uuids.startOf(System.currentTimeMillis() + 1000);

        cqlTemplate.execute("INSERT INTO chats_by_created (bucket, created_at, session_id, session_name) " +
                "VALUES ('all', ?, 's1', 'First Chat')", createdAt1);
        cqlTemplate.execute("INSERT INTO chats_by_created (bucket, created_at, session_id, session_name) " +
                "VALUES ('all', ?, 's2', 'Second Chat')", createdAt2);

        List<NoSqlChat> chats = chatRepository.findAll();

        assertThat(chats).hasSize(2);
        assertThat(chats.get(0).getId()).isEqualTo("s2");
        assertThat(chats.get(1).getId()).isEqualTo("s1");
    }

    @Test
    void shouldReturnAllConversationIds_fromAiChatMemory() {
        cqlTemplate.execute("INSERT INTO ai_chat_memory (session_id, session_name, created_at) VALUES ('s1', 'First', toTimestamp(now()))");
        cqlTemplate.execute("INSERT INTO ai_chat_memory (session_id, session_name, created_at) VALUES ('s2', 'Second', toTimestamp(now()))");

        List<String> ids = chatRepository.findConversationIds();

        assertThat(ids).containsExactlyInAnyOrder("s1", "s2");
    }

    @Test
    void shouldReturnPaginatedMessagesByConversationId() {
        String chatId = "s1";

        // Insert 10 messages, each with +1s timestamp
        long base = System.currentTimeMillis();
        for (int i = 1; i <= 10; i++) {
            cqlTemplate.execute("INSERT INTO ai_chat_message (session_id, msg_timestamp, msg_type, msg_content) " +
                            "VALUES (?, ?, ?, ?)",
                    chatId,
                    Instant.ofEpochMilli(base + (i * 1000L)),   // +1 second each
                    "user",
                    "Message-" + i
            );
        }

        // --- Page 1 ---
        ChatPage page1 = chatRepository.findByConversationId(chatId, new CursorMeta(null, 3));
        assertThat(page1.messages()).hasSize(3);
        assertThat(page1.messages().get(0).content()).isEqualTo("Message-10"); // DESC by timestamp
        assertThat(page1.messages().get(1).content()).isEqualTo("Message-9");
        assertThat(page1.messages().get(2).content()).isEqualTo("Message-8");

        // --- Page 2 ---
        ChatPage page2 = chatRepository.findByConversationId(chatId, page1.pageMeta());
        assertThat(page2.messages()).hasSize(3);
        assertThat(page2.messages().get(0).content()).isEqualTo("Message-7");
        assertThat(page2.messages().get(1).content()).isEqualTo("Message-6");
        assertThat(page2.messages().get(2).content()).isEqualTo("Message-5");

        // --- Page 3 ---
        ChatPage page3 = chatRepository.findByConversationId(chatId, page2.pageMeta());
        assertThat(page3.messages()).hasSize(3);
        assertThat(page3.messages().get(0).content()).isEqualTo("Message-4");
        assertThat(page3.messages().get(1).content()).isEqualTo("Message-3");
        assertThat(page3.messages().get(2).content()).isEqualTo("Message-2");

        // --- Page 4 ---
        ChatPage page4 = chatRepository.findByConversationId(chatId, page3.pageMeta());
        assertThat(page4.messages()).hasSize(1);
        assertThat(page4.messages().getFirst().content()).isEqualTo("Message-1");
    }

    @Test
    void shouldThrowWhenPageMetaIsNotCursorMeta() {
        String chatId = "s1";
        PageMeta wrongMeta = new OffsetMeta(0, 10, true); // not CursorMeta

        assertThatThrownBy(() -> chatRepository.findByConversationId(chatId, wrongMeta))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Expected CursorMeta but got OffsetMeta");
    }
}
