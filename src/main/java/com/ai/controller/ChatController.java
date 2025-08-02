package com.ai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatClient defaultSystemChatClient;
    private final ChatClient defaultUserChatClient;

    public ChatController(ChatClient defaultSystemChatClient, ChatClient defaultUserChatClient) {
        this.defaultSystemChatClient = defaultSystemChatClient;
        this.defaultUserChatClient = defaultUserChatClient;
    }

    /**
     * Streams AI responses to the client using Server-Sent Events (SSE).
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChat(@RequestParam String message) {
        return defaultSystemChatClient
                .prompt()
                .user(message)
                .stream()
                .content()
                .map(chunk -> {
                    String jsonChunk = "{\"text\": \"" + chunk.replace("\"", "\\\"") + "\"}";
                    return ServerSentEvent.builder(jsonChunk).build();
                })
                // after streaming finishes, emit an 'end' event
                .concatWith(Flux.just(
                        ServerSentEvent.<String>builder()
                                .event("end")
                                .build()
                ));
    }

    @GetMapping("/movies/by-composer")
    public Flux<ServerSentEvent<String>> getMoviesByComposer(@RequestParam String composer) {
        return defaultUserChatClient.prompt()
                .user(u -> u.param("composer", composer))
                .stream()
                .content()
                .map(chunk -> ServerSentEvent.builder(chunk).build());
    }

    // private final ChatMemory chatMemory;
    // private final VectorStore vectorStore;

   /* @GetMapping("/ask")
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
    }*/
}
