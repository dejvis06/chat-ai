package com.ai.infrastructure.config;

import com.ai.domain.entity.NoSqlChat;
import com.ai.infrastructure.repository.CassandraChatRepository;
import com.ai.infrastructure.repository.ChatRepository;
import com.datastax.oss.driver.api.core.CqlSession;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.core.cql.CqlTemplate;

@Configuration
public class ChatClientConfig {

    public static final String HELPFUL_ASSISTANT_PROMPT = "You are a helpful assistant.";
    public static final String NAME_GENERATION_PROMPT = "Generate a short, descriptive chat name based on the user prompt. Respond with the name only, no explanations or extra text.";

    /**
     * Creates a ChatClient bean preconfigured with the system prompt.
     *
     * @param chatModel the OpenAI chat model to use
     * @return a ChatClient instance with a default system prompt
     */
    @Bean
    ChatClient openAiChatClient(OpenAiChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem(HELPFUL_ASSISTANT_PROMPT)
                .build();
    }

    /**
     * Creates a ChatClient bean preconfigured with the chat-name generation system prompt.
     *
     * @param chatModel the OpenAI chat model to use
     * @return a ChatClient instance with a chat-name generation system prompt
     */
    @Bean
    ChatClient chatNameGeneratorClient(OpenAiChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem(NAME_GENERATION_PROMPT)
                .build();
    }

    @Bean
    CqlTemplate cqlTemplate(CqlSession session) {
        return new CqlTemplate(session);
    }

    /*@Bean
    ChatMemoryRepository chatMemoryRepository(CqlSession cqlSession) {
        return CassandraChatMemoryRepository
                .create(CassandraChatMemoryRepositoryConfig.builder()
                        .withCqlSession(cqlSession)
                        .build());
    }*/

    @Bean
    ChatRepository<NoSqlChat, String> chatRepository(ChatMemoryRepository cassandraChatMemoryRepository, CqlTemplate cqlTemplate) {
        return CassandraChatRepository.builder()
                .maxMessages(10)
                .chatMemoryRepository(cassandraChatMemoryRepository)
                .cqlTemplate(cqlTemplate)
                .build();
    }
}
