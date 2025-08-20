package com.ai;

import com.ai.config.CassandraTestConfig;
import com.ai.infrastructure.config.ChatClientConfig;
import com.ai.infrastructure.config.ChatMemoryConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import({CassandraTestConfig.class, ChatClientConfig.class, ChatMemoryConfig.class})
public abstract class BaseTest {
}
