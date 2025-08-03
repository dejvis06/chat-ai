package com.ai.controller;

import com.ai.application.ChatService;
import com.ai.application.OpenAiChatService;
import com.ai.application.dto.ChatMessageDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * Streams AI responses to the client using Server-Sent Events (SSE).
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream(@RequestParam String message) {
        return chatService.stream(message);
    }

    /**
     * Retrieves the full chat history for the given chat ID.
     */
    @GetMapping("/{chatId}")
    public List<ChatMessageDto> getChatHistory(@PathVariable String chatId) {
        return chatService.getChatHistory(chatId);
    }
}
