package com.ai.infrastructure.config;

import com.ai.domain.entity.Chat;
import com.ai.domain.entity.NoSqlChat;
import com.ai.infrastructure.repository.CassandraChatMemoryRepository;
import com.ai.infrastructure.repository.ChatRepository;
import com.ai.infrastructure.repository.RedisMessageWindowChatMemory;
import com.datastax.oss.driver.api.core.CqlSession;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.core.cql.CqlTemplate;

@Configuration
public class ChatMemoryConfig {

    @Bean
    ChatRepository<NoSqlChat> chatRepository(CqlTemplate cqlTemplate, CqlSession cqlSession) {
        return new CassandraChatMemoryRepository(cqlTemplate, cqlSession); // implements ChatCrudRepository<NoSqlChat>
    }

    @Bean
    public <T extends Chat> ChatMemory chatMemory(ChatRepository<T> chatRepository) {
        return RedisMessageWindowChatMemory.<T>builder()
                .chatRepository(chatRepository)
                .maxMessages(10)
                .build();
    }
}
