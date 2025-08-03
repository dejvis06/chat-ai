package com.ai.infrastructure.rest;

import com.ai.application.dto.ChatDto;
import com.ai.application.dto.ChatMessageDto;
import com.ai.application.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/chats")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * Creates a new chat based on the user’s initial prompt.
     *
     * <p>This endpoint generates a new chat record, assigns it an ID,
     * and returns the chat metadata along with an empty messages list.</p>
     *
     * @param userPrompt the first message or topic provided by the user to name or initialize the chat
     * @return a {@link ChatDto} containing the created chat’s details and an empty messages list
     */
    @PostMapping
    public ChatDto create(@RequestParam String userPrompt) {
        log.info("Creating new chat with initial user prompt: {}", userPrompt);
        return chatService.save(userPrompt);
    }

    /**
     * Streams AI responses to the client using Server-Sent Events (SSE).
     */
    @GetMapping(value = "/stream/{chatId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream(@PathVariable String chatId, @RequestParam String userPrompt) {
        log.info("Starting SSE stream for chatId={} with user prompt: {}", chatId, userPrompt);
        return chatService.stream(chatId, userPrompt);
    }

    /**
     * Retrieves the full chat history for the given chat ID.
     */
    @GetMapping("/{chatId}")
    public List<ChatMessageDto> getChatHistory(@PathVariable String chatId) {
        log.info("Fetching chat history for chatId={}", chatId);
        return chatService.getChatHistory(chatId);
    }

    /**
     * Retrieves all chats.
     *
     * @return a {@link ResponseEntity} containing a list of {@link ChatDto} objects
     */
    @GetMapping
    public ResponseEntity<List<ChatDto>> findAllChats() {
        log.info("Fetching all chats");

        List<ChatDto> chats = chatService.findAll();
        return ResponseEntity.ok(chats);
    }

    /**
     * Retrieves paginated messages for a specific chat, with pagination support.
     *
     * @param page   the zero-based page index (defaults to 0)
     * @param size   the number of messages to return per page (defaults to 10)
     * @param chatId the unique identifier of the chat whose messages should be retrieved
     * @return a {@link ResponseEntity} containing a list of {@link ChatMessageDto} for the specified chat
     */
    @GetMapping("/{chatId}/messages")
    public ResponseEntity<List<ChatMessageDto>> findAllMessagesByChatId(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @PathVariable String chatId
    ) {
        log.info("Fetching chats - page: {}, size: {}", page, size);

        List<ChatMessageDto> chats = chatService.findAllMessagesByChatId(chatId, page, size);
        return ResponseEntity.ok(chats);
    }
}
