package com.ai.application.service;

import com.ai.application.dto.ChatDto;
import com.ai.application.dto.ChatMessageDto;
import com.ai.domain.entity.Chat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatClient openAiChatClient;
    private final ChatClient chatNameGeneratorClient;
    private final ChatMemory chatMemory;
    private final JdbcChatMemoryRepository jdbcChatMemoryRepository;

    private final JdbcTemplate jdbcTemplate;

    public static final String END_STREAM = "END_STREAM";

    public ChatService(
            ChatClient openAiChatClient,
            ChatClient chatNameGeneratorClient,
            ChatMemory chatMemory,
            JdbcChatMemoryRepository jdbcChatMemoryRepository,
            JdbcTemplate jdbcTemplate
    ) {
        this.openAiChatClient = openAiChatClient;
        this.chatNameGeneratorClient = chatNameGeneratorClient;
        this.chatMemory = chatMemory;
        this.jdbcChatMemoryRepository = jdbcChatMemoryRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Creates and persists a new chat record based on the user’s first message,
     * initializes chat memory, and returns the created chat’s metadata.
     *
     * <p>Steps performed:
     * <ul>
     *   <li>Generates a chat name using the {@code chatNameGeneratorClient} from the user’s message.</li>
     *   <li>Inserts the chat (name and creation timestamp) into the database and retrieves the generated ID.</li>
     *   <li>Adds the user’s first message to the {@code chatMemory} for conversation continuity.</li>
     *   <li>Returns a {@link ChatDto} with chat metadata and no messages yet.</li>
     * </ul>
     *
     * @param userMessage the first message sent by the user to start the chat
     * @return a {@link ChatDto} containing the generated chat ID, chat name, creation timestamp, and empty message list
     */
    public ChatDto save(String userMessage) {
        String chatName = chatNameGeneratorClient.prompt()
                .user(userMessage)
                .call()
                .content();

        KeyHolder keyHolder = new GeneratedKeyHolder();
        Instant createdAt = Instant.now();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO chat (name, created_at) VALUES (?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, chatName);
            ps.setTimestamp(2, Timestamp.from(createdAt));
            return ps;
        }, keyHolder);

        // Get the auto-generated id
        String chatId = keyHolder.getKey().toString();

        return new ChatDto(
                chatId,
                chatName,
                createdAt,
                null
        );
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

        chatMemory.add(chatId, new UserMessage(userMessage));
        log.info("User message added to chat memory for chatId={}", chatId);

        StringBuilder assistantResponse = new StringBuilder();

        return openAiChatClient
                .prompt()
                .messages(chatMemory.get(chatId))
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
                    chatMemory.add(chatId, new AssistantMessage(assistantResponse.toString()));
                    log.info("Assistant response saved to chat memory for chatId={}", chatId);

                    return Flux.just(
                            ServerSentEvent.<String>builder()
                                    .event(END_STREAM)
                                    .build()
                    );
                }));
    }

    private String encodeToJson(String message) {
        return "{\"text\": \"" + message.replace("\"", "\\\"") + "\"}";
    }

    /**
     * Retrieves the full chat history for the given chat ID.
     *
     * @param chatId the unique identifier of the chat session
     * @return a list of chat messages (as DTOs) exchanged in this chat
     */
    public List<ChatMessageDto> getChatHistory(String chatId) {
        return chatMemory.get(chatId)
                .stream()
                .map(ChatMessageDto::from)
                .toList();
    }
}

