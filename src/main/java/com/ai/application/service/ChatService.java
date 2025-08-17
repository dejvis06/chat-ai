package com.ai.application.service;

import com.ai.application.dto.ChatDto;
import com.ai.application.dto.ChatMessageDto;
import com.ai.domain.entity.Chat;
import com.ai.domain.model.pagination.ChatPage;
import com.ai.domain.model.pagination.PageMeta;
import com.ai.infrastructure.repository.ChatRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatClient openAiChatClient;
    private final ChatClient chatNameGeneratorClient;
    private final ChatMemory chatMemory;
    private final ChatRepository<? extends Chat> chatRepository;
    private final ObjectMapper objectMapper;

    public static final String CHAT_CREATED = "chat_created";
    public static final String END_STREAM = "END_STREAM";

    public ChatService(
            ChatClient openAiChatClient,
            ChatClient chatNameGeneratorClient,
            ChatMemory chatMemory,
            ChatRepository<? extends Chat> chatRepository, ObjectMapper objectMapper
    ) {
        this.openAiChatClient = openAiChatClient;
        this.chatNameGeneratorClient = chatNameGeneratorClient;
        this.chatMemory = chatMemory;
        this.chatRepository = chatRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Streams an assistant response over Server-Sent Events (SSE).
     *
     * <p>Workflow:</p>
     * <ul>
     *   <li>If {@code chatId} is null, a new chat is created and a {@code chat_created} SSE event is sent first.</li>
     *   <li>The user's message is added to the chat memory.</li>
     *   <li>The assistant's response is requested from the model and streamed back chunk by chunk as SSE events.</li>
     *   <li>All streamed chunks are accumulated and, once complete, the full assistant message is saved to chat memory.</li>
     *   <li>Finally, an {@code END_STREAM} SSE event signals completion of the stream.</li>
     * </ul>
     *
     * @param chatId      existing chat identifier, or {@code null} to create a new chat
     * @param userMessage the message from the user to process
     * @return a {@link Flux} of {@link ServerSentEvent} objects representing the streamed response
     */
    public Flux<ServerSentEvent<String>> stream(String chatId, String userMessage) {
        log.info("Received user message: {}", userMessage);

        final String finalChatId = (chatId == null)
                ? saveChat(userMessage)
                : chatId;
        final boolean createdChat = (chatId == null);

        Flux<ServerSentEvent<String>> createdChatEvent = createdChat
                ? Flux.just(ServerSentEvent.builder(finalChatId).event(CHAT_CREATED).build())
                : Flux.empty();

        chatMemory.add(finalChatId, new UserMessage(userMessage));
        log.info("User message added to chat memory for chatId={}", finalChatId);

        StringBuilder assistantResponse = new StringBuilder();

        return createdChatEvent.concatWith(
                openAiChatClient
                        .prompt()
                        .messages(chatMemory.get(finalChatId))
                        .stream()
                        .content()
                        .map(chunk -> {
                            log.info("Streaming chunk: {}", chunk);

                            assistantResponse.append(chunk);  // accumulate the streamed chunk

                            String jsonChunk = encodeToJson(chunk); // convert to JSON for SSE
                            log.info("Encoded chunk to JSON");

                            return ServerSentEvent.builder(jsonChunk).build();  // SSE emit
                        })
                        .concatWith(Flux.defer(() -> {
                            log.info("Streaming complete");

                            // Add full assistant response
                            chatMemory.add(finalChatId, new AssistantMessage(assistantResponse.toString()));
                            log.info("Assistant response saved to chat memory for chatId={}", finalChatId);

                            return Flux.just(
                                    ServerSentEvent.<String>builder()
                                            .event(END_STREAM)
                                            .build()
                            );
                        }))
        );
    }

    private String saveChat(String userMessage) {
        log.info("Starting chat creation for user message: {}", userMessage);

        String chatName = chatNameGeneratorClient.prompt()
                .user(userMessage)
                .call()
                .content();
        log.info("Generated chat name from client: {}", chatName);

        String chatId = chatRepository.save(chatName).getId();
        Assert.hasText(chatId, "chatId cannot be empty or null");
        log.info("Chat successfully created with ID: {}", chatId);

        return chatId;
    }

    private String encodeToJson(String message) {
        try {
            return objectMapper.writeValueAsString(Map.of("text", message));
        } catch (JsonProcessingException e) {
            log.error("Failed to encode message '{}' to JSON", message, e);
            throw new RuntimeException("Failed to encode JSON", e);
        }
    }

    /**
     * Retrieves the full chat history for the given chat ID.
     *
     * @param chatId the unique identifier of the chat session
     * @return a list of chat messages (as DTOs) exchanged in this chat
     */
    public List<ChatMessageDto> getChatHistory(String chatId) {
        log.info("Fetching chat history for chatId: {}", chatId);

        List<ChatMessageDto> history = chatMemory.get(chatId)
                .stream()
                .map(ChatMessageDto::from)
                .toList();

        log.info("Successfully fetched chat history for chatId: {}", chatId);

        return history;
    }

    /**
     * Retrieves all chats.
     *
     * @return a {@link ResponseEntity} containing a list of {@link ChatDto} objects
     */
    public List<ChatDto> findAll() {
        log.info("Fetching all chats from repository");

        List<? extends Chat> chats = chatRepository.findAll();
        log.info("Retrieved {} chats from database", chats.size());

        List<ChatDto> result = chats.stream()
                .map(chat -> ChatDto.from(chat, null))
                .toList();

        log.info("Successfully fetched all chats");

        return result;
    }

    /**
     * Retrieves a paginated list of messages for the specified chat.
     *
     * @param chatId   the unique identifier of the chat (conversation ID)
     * @param pageMeta page metadata
     * @return an object of {@link ChatPage} representing the requested messages and the next available page
     */
    public ChatPage findMessagesByChatId(String chatId, PageMeta pageMeta) {
        log.info("Fetching messages for chatId={} and page metadata={}", chatId, pageMeta);

        ChatPage chatPage = chatRepository.findByConversationId(chatId, pageMeta);
        log.info("Retrieved {} messages from repository for chatId={}", chatPage.messages().size(), chatId);

        return chatPage;
    }
}

