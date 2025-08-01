/*
package com.ai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class ChatController {

    private final ChatClient defaultSystemChatClient;
    private final ChatClient defaultUserChatClient;
    private final ChatMemory chatMemory;
    private final VectorStore vectorStore;

    public ChatController(ChatClient defaultSystemChatClient, ChatClient defaultUserChatClient, ChatMemory chatMemory, VectorStore vectorStore) {
        this.defaultSystemChatClient = defaultSystemChatClient;
        this.defaultUserChatClient = defaultUserChatClient;
        this.chatMemory = chatMemory;
        this.vectorStore = vectorStore;
    }

    */
/**
     * Streams AI responses to the client using Server-Sent Events (SSE).
     *//*

    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChat(@RequestParam String message) {
        return defaultSystemChatClient
                .prompt()
                .user(message)
                .stream()
                .content()
                .map(chunk -> ServerSentEvent.builder(chunk).build());
    }

    @GetMapping("/movies/by-composer")
    public Flux<ServerSentEvent<String>> getMoviesByComposer(@RequestParam String composer) {
        return defaultUserChatClient.prompt()
                .user(u -> u.param("composer", composer))
                .stream()
                .content()
                .map(chunk -> ServerSentEvent.builder(chunk).build());
    }

    @GetMapping("/ask")
    public String ask(@RequestParam String userText) {

        return defaultSystemChatClient
                .prompt()
                .advisors(
                        // 1️⃣ Adds conversation history to the prompt
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),

                        // 2️⃣ Uses the conversation history to enrich the answer
                        QuestionAnswerAdvisor.builder(vectorStore).build()
                )
                .user(userText)
                .call()
                .content();
    }

}
*/
