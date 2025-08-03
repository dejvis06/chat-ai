package com.ai.application.service;

import com.ai.application.dto.ChatMessageDto;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.List;

public interface ChatService {

    /**
     * Streams chat responses as Server-Sent Events.
     *
     * @param message The user message to send to AI.
     */
    Flux<ServerSentEvent<String>> stream(String message);

    default String encodeToJson(String message) {
        return "{\"text\": \"" + message.replace("\"", "\\\"") + "\"}";
    }

    List<ChatMessageDto> getChatHistory(String chatId);
}
