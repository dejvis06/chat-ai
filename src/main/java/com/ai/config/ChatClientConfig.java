package com.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Scanner;

@Configuration
public class ChatClientConfig {

    public static final String SYSTEM_PROMPT = "You are a helpful assistant.";
    public static final String DEFAULT_PROMPT = "Tell me the names of 5 movies whose soundtrack was composed by {composer}";

    /**
     * Creates a ChatClient bean preconfigured with the system prompt.
     *
     * @param chatModel the OpenAI chat model to use
     * @return a ChatClient instance with a default system prompt
     */
    @Bean
    public ChatClient defaultSystemChatClient(OpenAiChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }

    /**
     * ChatClient configured for dynamic prompting.
     * This variant doesn’t set a fixed system prompt —
     * instead, prompts are dynamically built with parameters at runtime.
     *
     * @param chatModel the OpenAI chat model to use
     * @return a ChatClient instance with a default user prompt
     */
    @Bean
    public ChatClient defaultUserChatClient(OpenAiChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultUser(DEFAULT_PROMPT)
                .build();
    }

    /*@Bean
    CommandLineRunner cli(
            @Qualifier("openAiChatClient") ChatClient openAiChatClient,
            @Qualifier("anthropicChatClient") ChatClient anthropicChatClient) {

        return args -> {
            var scanner = new Scanner(System.in);
            ChatClient chat;

            // Model selection
            System.out.println("\nSelect your AI model:");
            System.out.println("1. OpenAI");
            System.out.println("2. Anthropic");
            System.out.print("Enter your choice (1 or 2): ");

            String choice = scanner.nextLine().trim();

            if (choice.equals("1")) {
                chat = openAiChatClient;
                System.out.println("Using OpenAI model");
            } else {
                chat = anthropicChatClient;
                System.out.println("Using Anthropic model");
            }

            // Use the selected chat client
            System.out.print("\nEnter your question: ");
            String input = scanner.nextLine();
            String response = chat.prompt(input).call().content();
            System.out.println("ASSISTANT: " + response);

            scanner.close();
        };
    }*/
}
