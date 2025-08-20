package com.ai;

import com.ai.config.CassandraTestConfig;
import com.ai.infrastructure.config.ChatClientConfig;
import com.ai.infrastructure.config.ChatMemoryConfig;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.cassandra.core.cql.CqlTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import({CassandraTestConfig.class, ChatClientConfig.class, ChatMemoryConfig.class})
public abstract class BaseTest {

    @Autowired
    protected CqlTemplate cqlTemplate;

    @AfterEach
    protected void cleanUp() {
        cqlTemplate.execute("TRUNCATE ai_chat_message");
        cqlTemplate.execute("TRUNCATE ai_chat_memory");
        cqlTemplate.execute("TRUNCATE chats_by_created");
    }
}
