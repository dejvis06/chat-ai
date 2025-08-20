package com.ai.infrastructure.rest;

import com.ai.application.dto.ChatDto;
import com.ai.application.dto.ChatMessageDto;
import com.ai.application.service.ChatService;
import com.ai.domain.model.pagination.ChatPage;
import com.ai.domain.model.pagination.PageMeta;
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
     * Streams AI responses to the client using Server-Sent Events (SSE).
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream(@RequestParam(required = false) String chatId, @RequestParam String userPrompt) {
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
     * @param pageMeta page metadata
     * @param chatId   the unique identifier of the chat whose messages should be retrieved
     * @return a {@link ResponseEntity} containing an object {@link ChatPage} representing the requested messages and the next available page
     */
    @GetMapping("/{chatId}/messages")
    public ResponseEntity<ChatPage> findMessagesByChatId(
            @RequestBody PageMeta pageMeta,
            @PathVariable String chatId
    ) {
        log.info("Fetching chats - page meta: {}", pageMeta);
        return ResponseEntity.ok(chatService.findMessagesByChatId(chatId, pageMeta));
    }
}
