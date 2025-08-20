package com.ai.application.service;

import com.ai.BaseTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.cassandra.core.cql.CqlTemplate;
import org.springframework.http.codec.ServerSentEvent;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ChatServiceTest extends BaseTest {

    @Autowired
    ChatService chatService;

    @Autowired
    CqlTemplate cqlTemplate;

    @AfterEach
    void cleanUp() {
        cqlTemplate.execute("TRUNCATE ai_chat_message");
        cqlTemplate.execute("TRUNCATE ai_chat_memory");
        cqlTemplate.execute("TRUNCATE chats_by_created");
    }

    @Test
    void stream_shouldReturnEventsFromRealCassandra() {
        List<ServerSentEvent<String>> events =
                chatService.stream(null, "hi")
                        .collectList()   // gather all emitted SSEs
                        .block();        // wait for completion

        assertThat(events).isNotEmpty();
        assertThat(events.getFirst().event()).isEqualTo(ChatService.CHAT_CREATED);
        // you can also check last element is END_STREAM if your service emits it
        assertThat(events.get(events.size() - 1).event()).isEqualTo(ChatService.END_STREAM);
    }
}
