package com.ai.application.service;

import com.ai.application.dto.ChatDto;
import com.ai.application.dto.ChatMessageDto;
import com.ai.domain.entity.Chat;
import com.ai.infrastructure.repository.ChatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatClient openAiChatClient;
    private final ChatClient chatNameGeneratorClient;
    private final ChatMemory chatMemory;

    private final ChatRepository chatRepository;

    public static final String END_STREAM = "END_STREAM";

    public ChatService(
            ChatClient openAiChatClient,
            ChatClient chatNameGeneratorClient,
            ChatMemory chatMemory,
            ChatRepository chatRepository
    ) {
        this.openAiChatClient = openAiChatClient;
        this.chatNameGeneratorClient = chatNameGeneratorClient;
        this.chatMemory = chatMemory;
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
     * @param chatId the unique identifier of the chat (conversation ID)
     * @param page   the zero-based page index (0 returns the first page)
     * @param size   the maximum number of messages to return per page
     * @return a list of {@link ChatMessageDto} objects representing the requested messages
     */
    public List<ChatMessageDto> findAllMessagesByChatId(String chatId, int page, int size) {
        log.info("Fetching messages for chatId={} (page={}, size={})", chatId, page, size);

        List<Message> messages = chatRepository.findAllMessagesByChatId(chatId, page, size);
        log.debug("Retrieved {} messages from repository for chatId={}", messages.size(), chatId);

        List<ChatMessageDto> result = messages.stream()
                .map(ChatMessageDto::from)
                .toList();

        log.info("Mapped {} messages to ChatMessageDto for chatId={}", result.size(), chatId);
        return result;
    }
}

