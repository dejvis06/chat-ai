package com.ai.infrastructure.repository;

import com.ai.config.CassandraTestConfig;
import com.ai.domain.entity.NoSqlChat;
import com.ai.infrastructure.config.ChatClientConfig;
import com.ai.infrastructure.config.ChatMemoryConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.cassandra.CassandraContainer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import({CassandraTestConfig.class, ChatClientConfig.class, ChatMemoryConfig.class})
class CassandraChatRepositoryIT {

    @Autowired
    CassandraContainer cassandraContainer;

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

        // verify it exists in DB
        var allChats = chatRepository.findAll();
        assertThat(allChats)
                .extracting(NoSqlChat::getId)
                .contains(saved.getId());
    }
}
