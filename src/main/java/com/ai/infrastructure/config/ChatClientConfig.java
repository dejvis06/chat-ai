package com.ai.infrastructure.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.cassandra.CassandraChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.cassandra.CassandraChatMemoryRepositoryConfig;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

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
    public ChatClient openAiChatClient(OpenAiChatModel chatModel) {
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
    public ChatClient chatNameGeneratorClient(OpenAiChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem(NAME_GENERATION_PROMPT)
                .build();
    }

    /**
     * Creates a Cassandra-backed ChatMemoryRepository with a fixed TTL.
     *
     * @return a ChatMemoryRepository instance configured to store chat memory
     *         in Cassandra with entries expiring after 1 day.
     */
    @Bean
    public ChatMemoryRepository chatMemoryRepository() {
        return  CassandraChatMemoryRepository.create(
                CassandraChatMemoryRepositoryConfig.builder()
                        .withTimeToLive(Duration.ofDays(1))
                        .build()
        );
    }

    /**
     * Creates a ChatMemory bean backed by the provided ChatMemoryRepository.
     *
     * Configured as a message window memory that retains only the most recent
     * 10 messages for each conversation.
     *
     * @param chatMemoryRepository the repository used for storing chat history
     * @return a ChatMemory instance with a 10-message retention window
     */
    @Bean
    public ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .maxMessages(10)
                .chatMemoryRepository(chatMemoryRepository)
                .build();
    }
}
