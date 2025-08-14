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
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatClient openAiChatClient;
    private final ChatClient chatNameGeneratorClient;

    private final ChatRepository<Chat, Object> chatRepository;

    public static final String END_STREAM = "END_STREAM";

    public ChatService(
            ChatClient openAiChatClient,
            ChatClient chatNameGeneratorClient,
            ChatRepository chatRepository
    ) {
        this.openAiChatClient = openAiChatClient;
        this.chatNameGeneratorClient = chatNameGeneratorClient;
        this.chatRepository = chatRepository;
    }

    /**
     * Creates and persists a new Chat based on the provided user message.
     *
     * @param userMessage the initial message from the user, used to generate a chat name
     * @return a {@link ChatDto} containing the chat’s ID, generated name, and no messages
     */
    public ChatDto save(String userMessage) {
        log.info("Starting chat creation for user message: {}", userMessage);

        String chatName = chatNameGeneratorClient.prompt()
                .user(userMessage)
                .call()
                .content();
        log.debug("Generated chat name from client: {}", chatName);

        Chat chat = chatRepository.save(chatName);
        log.info("Chat successfully saved with ID: {}", chat.getId());

        return ChatDto.from(chat, null);
    }

    /**
     * Streams the assistant’s response to the given user message as Server‑Sent Events (SSE).
     * <p>
     * Steps:
     * <ul>
     *   <li>Sends the message to OpenAI and streams back partial response chunks as SSE.</li>
     *   <li>Accumulates all chunks into a single assistant reply.</li>
     *   <li>When streaming finishes, stores the full assistant response in chat memory and emits a final “end” event.</li>
     * </ul>
     *
     * @param userMessage the user’s message to send to the assistant
     * @return a {@code Flux<ServerSentEvent<String>>} emitting streamed response chunks followed by a final “end” event
     */
    public Flux<ServerSentEvent<String>> stream(String chatId, String userMessage) {
        log.info("Received user message: {}", userMessage);

        chatRepository.add(chatId, new UserMessage(userMessage));
        log.info("User message added to chat memory for chatId={}", chatId);

        StringBuilder assistantResponse = new StringBuilder();

        return openAiChatClient
                .prompt()
                .messages(chatRepository.get(chatId))
                .user(userMessage)
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
                    chatRepository.add(chatId, new AssistantMessage(assistantResponse.toString()));
                    log.info("Assistant response saved to chat memory for chatId={}", chatId);

                    return Flux.just(
                            ServerSentEvent.<String>builder()
                                    .event(END_STREAM)
                                    .build()
                    );
                }));
    }

    private String encodeToJson(String message) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(Map.of("text", message));
        } catch (JsonProcessingException e) {
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
        return chatRepository.get(chatId)
                .stream()
                .map(ChatMessageDto::from)
                .toList();
    }

    /**
     * Retrieves all chats.
     *
     * @return a {@link ResponseEntity} containing a list of {@link ChatDto} objects
     */
    public List<ChatDto> findAll() {
        log.info("Fetching all chats from repository");

        List<Chat> chats = chatRepository.findAll();
        log.debug("Retrieved {} chats from database", chats.size());

        return chats.stream()
                .map(chat -> ChatDto.from(chat, null))
                .toList();
    }

    /**
     * Retrieves a paginated list of messages for the specified chat.
     *
     * @param chatId   the unique identifier of the chat (conversation ID)
     * @param pageMeta page metadata
     * @return an object of {@link ChatPage} representing the requested messages and the next available page
     */
    public ChatPage findAllMessagesByChatId(String chatId, PageMeta pageMeta) {
        log.info("Fetching messages for chatId={} and page metadata={}", chatId, pageMeta);

        ChatPage chatPage = chatRepository.findAll(chatId, pageMeta);
        log.debug("Retrieved {} messages from repository for chatId={}", chatPage.messages().size(), chatId);

        return chatPage;
    }
}

