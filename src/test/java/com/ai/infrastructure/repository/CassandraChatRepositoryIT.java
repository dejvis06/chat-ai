package com.ai.infrastructure.repository;

import com.ai.config.CassandraTestConfig;
import com.ai.domain.entity.NoSqlChat;
import com.ai.infrastructure.config.ChatClientConfig;
import com.ai.infrastructure.config.ChatMemoryConfig;
import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.cassandra.core.cql.CqlTemplate;
import org.testcontainers.cassandra.CassandraContainer;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import({CassandraTestConfig.class, ChatClientConfig.class, ChatMemoryConfig.class})
class CassandraChatRepositoryIT {

    @Autowired
    CassandraContainer cassandraContainer;

    @Autowired
    CassandraChatMemoryRepository chatRepository;

    @Autowired
    CqlTemplate cqlTemplate;

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
                "SELECT session_id, session_name FROM ai_chat_memory WHERE session_id = ?",
                (row, rowNum) -> Map.of(
                        "id", row.getString("session_id"),
                        "name", row.getString("session_name")
                ),
                saved.getId()
        );
        assertThat(dbChat.get("id")).isEqualTo(saved.getId());
        assertThat(dbChat.get("name")).isEqualTo(saved.getName());

        // verify it exists in chats_by_created
        UUID createdAtTimeUuid = Uuids.startOf(saved.getCreatedAt().toEpochMilli());
        var createdChat = cqlTemplate.queryForObject(
                "SELECT session_id, session_name, created_at " +
                        "FROM chats_by_created " +
                        "WHERE bucket = 'all' AND created_at = ? AND session_id = ?",
                (row, rowNum) -> Map.of(
                        "id", row.getString("session_id"),
                        "name", row.getString("session_name")
                ),
                createdAtTimeUuid,
                saved.getId()
        );
        assertThat(createdChat.get("id")).isEqualTo(saved.getId());
        assertThat(createdChat.get("name")).isEqualTo(saved.getName());
    }

}
