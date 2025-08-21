package com.ai.infrastructure.rest;

import com.ai.BaseTest;
import com.ai.application.dto.ChatDto;
import com.ai.application.dto.ChatMessageDto;
import com.ai.application.service.ChatService;
import com.ai.domain.model.pagination.ChatPage;
import com.ai.domain.model.pagination.CursorMeta;
import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ChatControllerTest extends BaseTest {

    @Autowired
    ChatController chatController;

    @Test
    void stream_shouldReturnEventsFromRealCassandra() {
        List<ServerSentEvent<String>> events =
                chatController.stream(null, "hi")
                        .collectList()   // gather all emitted SSEs
                        .block();        // wait for completion

        assertThat(events).isNotEmpty();
        assertThat(events.getFirst().event()).isEqualTo(ChatService.CHAT_CREATED);
        assertThat(events.getLast().event()).isEqualTo(ChatService.END_STREAM);
    }

    @Test
    void getChatHistory_returnsDescendingMessages() {
        String chatId = "s1";
        long base = System.currentTimeMillis();

        // seed ai_chat_message with +1s timestamps
        cqlTemplate.execute(
                "INSERT INTO ai_chat_message (session_id, msg_timestamp, msg_type, msg_content) VALUES (?, ?, ?, ?)",
                chatId, Instant.ofEpochMilli(base + 1000), "user", "Message-1"
        );
        cqlTemplate.execute(
                "INSERT INTO ai_chat_message (session_id, msg_timestamp, msg_type, msg_content) VALUES (?, ?, ?, ?)",
                chatId, Instant.ofEpochMilli(base + 2000), "assistant", "Message-2"
        );
        cqlTemplate.execute(
                "INSERT INTO ai_chat_message (session_id, msg_timestamp, msg_type, msg_content) VALUES (?, ?, ?, ?)",
                chatId, Instant.ofEpochMilli(base + 3000), "user", "Message-3"
        );

        // call controller directly
        List<ChatMessageDto> result = chatController.getChatHistory(chatId);

        // assert messages come back DESC by timestamp
        assertThat(result).hasSize(3);
        assertThat(result.get(0).content()).isEqualTo("Message-3");
        assertThat(result.get(1).content()).isEqualTo("Message-2");
        assertThat(result.get(2).content()).isEqualTo("Message-1");
    }

    @Test
    void findAllChats_returnsChatsInDescOrder() {
        String s1 = "s1";
        String s2 = "s2";
        long now = System.currentTimeMillis();

        UUID older = Uuids.startOf(now);          // older
        UUID newer = Uuids.startOf(now + 1000L);  // newer

        // seed chats_by_created (bucket='all')
        cqlTemplate.execute(
                "INSERT INTO chats_by_created (bucket, created_at, session_id, session_name) VALUES ('all', ?, ?, ?)",
                older, s1, "First Chat"
        );
        cqlTemplate.execute(
                "INSERT INTO chats_by_created (bucket, created_at, session_id, session_name) VALUES ('all', ?, ?, ?)",
                newer, s2, "Second Chat"
        );

        // call controller
        ResponseEntity<List<ChatDto>> resp = chatController.findAllChats();
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();

        List<ChatDto> body = resp.getBody();
        assertThat(body).hasSize(2);
        // DESC by created_at -> newer first
        assertThat(body.get(0).id()).isEqualTo(s2);
        assertThat(body.get(0).name()).isEqualTo("Second Chat");
        assertThat(body.get(1).id()).isEqualTo(s1);
        assertThat(body.get(1).name()).isEqualTo("First Chat");
    }

    @Test
    void shouldReturnPaginatedMessagesFromController() {
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
        ResponseEntity<ChatPage> resp1 =
                chatController.findMessagesByChatId(new CursorMeta(null, 3), chatId);
        ChatPage page1 = resp1.getBody();
        assertThat(page1.messages()).hasSize(3);
        assertThat(page1.messages().get(0).content()).isEqualTo("Message-10");
        assertThat(page1.messages().get(1).content()).isEqualTo("Message-9");
        assertThat(page1.messages().get(2).content()).isEqualTo("Message-8");

        // --- Page 2 ---
        ResponseEntity<ChatPage> resp2 =
                chatController.findMessagesByChatId(page1.pageMeta(), chatId);
        ChatPage page2 = resp2.getBody();
        assertThat(page2.messages()).hasSize(3);
        assertThat(page2.messages().get(0).content()).isEqualTo("Message-7");
        assertThat(page2.messages().get(1).content()).isEqualTo("Message-6");
        assertThat(page2.messages().get(2).content()).isEqualTo("Message-5");

        // --- Page 3 ---
        ResponseEntity<ChatPage> resp3 =
                chatController.findMessagesByChatId(page2.pageMeta(), chatId);
        ChatPage page3 = resp3.getBody();
        assertThat(page3.messages()).hasSize(3);
        assertThat(page3.messages().get(0).content()).isEqualTo("Message-4");
        assertThat(page3.messages().get(1).content()).isEqualTo("Message-3");
        assertThat(page3.messages().get(2).content()).isEqualTo("Message-2");

        // --- Page 4 ---
        ResponseEntity<ChatPage> resp4 =
                chatController.findMessagesByChatId(page3.pageMeta(), chatId);
        ChatPage page4 = resp4.getBody();
        assertThat(page4.messages()).hasSize(1);
        assertThat(page4.messages().getFirst().content()).isEqualTo("Message-1");
    }
}
