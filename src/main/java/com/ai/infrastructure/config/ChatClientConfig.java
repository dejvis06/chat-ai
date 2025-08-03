package com.ai.infrastructure.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    public static final String SYSTEM_PROMPT = "You are a helpful assistant.";

    /**
     * Creates a ChatClient bean preconfigured with the system prompt.
     *
     * @param chatModel the OpenAI chat model to use
     * @return a ChatClient instance with a default system prompt
     */
    @Bean
    public ChatClient openAiChatClient(OpenAiChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }
}
