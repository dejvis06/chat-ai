package com.ai.application.service;

import com.ai.BaseTest;
import com.ai.application.dto.ChatDto;
import com.ai.application.dto.ChatMessageDto;
import com.ai.domain.model.pagination.ChatPage;
import com.ai.domain.model.pagination.CursorMeta;
import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.cassandra.core.cql.CqlTemplate;
import org.springframework.http.codec.ServerSentEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ChatServiceTest extends BaseTest {

    @Autowired
    ChatService chatService;

    @Test
    void stream_shouldReturnEventsFromRealCassandra() {
        List<ServerSentEvent<String>> events =
                chatService.stream(null, "hi")
                        .collectList()   // gather all emitted SSEs
                        .block();        // wait for completion

        assertThat(events).isNotEmpty();
        assertThat(events.getFirst().event()).isEqualTo(ChatService.CHAT_CREATED);
        assertThat(events.getLast().event()).isEqualTo(ChatService.END_STREAM);
    }

    @Test
    void getChatHistory_shouldReturnMessagesInDescendingOrder() {
        String chatId = "chat-123";

        // Insert 2 messages with different timestamps
        Instant now = Instant.now();
        cqlTemplate.execute(
                "INSERT INTO ai_chat_message (session_id, msg_timestamp, msg_type, msg_content) VALUES (?, ?, ?, ?)",
                chatId, now, "user", "Hello"
        );
        cqlTemplate.execute(
                "INSERT INTO ai_chat_message (session_id, msg_timestamp, msg_type, msg_content) VALUES (?, ?, ?, ?)",
                chatId, now.plusSeconds(1), "assistant", "Hi there!"
        );

        // Call service
        List<ChatMessageDto> history = chatService.getChatHistory(chatId);

        // Assert
        assertThat(history).hasSize(2);
        assertThat(history.get(0).content()).isEqualTo("Hi there!"); // newest first
        assertThat(history.get(1).content()).isEqualTo("Hello");
    }

    @Test
    void findAll_shouldReturnChatsOrderedByCreatedAtDesc() {
        // seed
        String s1 = "s1";
        String s2 = "s2";
        long now = System.currentTimeMillis();

        UUID t1 = Uuids.startOf(now);             // older
        UUID t2 = Uuids.startOf(now + 1000);      // newer

        cqlTemplate.execute(
                "INSERT INTO chats_by_created (bucket, created_at, session_id, session_name) VALUES ('all', ?, ?, ?)",
                t1, s1, "First Chat"
        );
        cqlTemplate.execute(
                "INSERT INTO chats_by_created (bucket, created_at, session_id, session_name) VALUES ('all', ?, ?, ?)",
                t2, s2, "Second Chat"
        );

        // act
        List<ChatDto> result = chatService.findAll();

        // assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(s2);
        assertThat(result.get(0).name()).isEqualTo("Second Chat");
        assertThat(result.get(1).id()).isEqualTo(s1);
        assertThat(result.get(1).name()).isEqualTo("First Chat");
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
        ChatPage page1 = chatService.findMessagesByChatId(chatId, new CursorMeta(null, 3));
        assertThat(page1.messages()).hasSize(3);
        assertThat(page1.messages().get(0).content()).isEqualTo("Message-10"); // DESC by timestamp
        assertThat(page1.messages().get(1).content()).isEqualTo("Message-9");
        assertThat(page1.messages().get(2).content()).isEqualTo("Message-8");

        // --- Page 2 ---
        ChatPage page2 = chatService.findMessagesByChatId(chatId, page1.pageMeta());
        assertThat(page2.messages()).hasSize(3);
        assertThat(page2.messages().get(0).content()).isEqualTo("Message-7");
        assertThat(page2.messages().get(1).content()).isEqualTo("Message-6");
        assertThat(page2.messages().get(2).content()).isEqualTo("Message-5");

        // --- Page 3 ---
        ChatPage page3 = chatService.findMessagesByChatId(chatId, page2.pageMeta());
        assertThat(page3.messages()).hasSize(3);
        assertThat(page3.messages().get(0).content()).isEqualTo("Message-4");
        assertThat(page3.messages().get(1).content()).isEqualTo("Message-3");
        assertThat(page3.messages().get(2).content()).isEqualTo("Message-2");

        // --- Page 4 ---
        ChatPage page4 = chatService.findMessagesByChatId(chatId, page3.pageMeta());
        assertThat(page4.messages()).hasSize(1);
        assertThat(page4.messages().getFirst().content()).isEqualTo("Message-1");
    }
}
