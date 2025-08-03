package com.ai.application.service;

import com.ai.application.dto.ChatMessageDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

@Service
public class OpenAiChatService implements ChatService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiChatService.class);

    public static final String END_STREAM = "END_STREAM";
    private final ChatClient openAiChatClient;
    private final ChatMemory chatMemory;

    private static final String CHAT_ID = UUID.randomUUID().toString();

    public OpenAiChatService(ChatClient openAiChatClient, ChatMemory chatMemory) {
        this.openAiChatClient = openAiChatClient;
        this.chatMemory = chatMemory;
    }

    /**
     * Streams the assistant’s response to the given user message as Server‑Sent Events (SSE).
     * <p>
     * Steps:
     * <ul>
     *   <li>Adds the user message to chat memory for context.</li>
     *   <li>Sends the message to OpenAI and streams back partial response chunks as SSE.</li>
     *   <li>Accumulates all chunks into a single assistant reply.</li>
     *   <li>When streaming finishes, stores the full assistant response in chat memory and emits a final “end” event.</li>
     * </ul>
     *
     * @param message the user’s message to send to the assistant
     * @return a {@code Flux<ServerSentEvent<String>>} emitting streamed response chunks followed by a final “end” event
     */
    public Flux<ServerSentEvent<String>> stream(String message) {
        log.info("Received user message: {}", message);

        chatMemory.add(CHAT_ID, new UserMessage(message));
        log.info("User message added to chat memory for chatId={}", CHAT_ID);

        StringBuilder assistantResponse = new StringBuilder();

        return openAiChatClient
                .prompt()
                .messages(chatMemory.get(CHAT_ID))
                .user(message)
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
                    chatMemory.add(CHAT_ID, new AssistantMessage(assistantResponse.toString()));
                    log.info("Assistant response saved to chat memory for chatId={}", CHAT_ID);

                    return Flux.just(
                            ServerSentEvent.<String>builder()
                                    .event(END_STREAM)
                                    .build()
                    );
                }));
    }

    /**
     * Retrieves the full chat history for the given chat ID.
     *
     * @param chatId the unique identifier of the chat session
     * @return a list of chat messages (as DTOs) exchanged in this chat
     */
    @Override
    public List<ChatMessageDto> getChatHistory(String chatId) {
        return chatMemory.get(chatId)
                .stream()
                .map(ChatMessageDto::from)
                .toList();
    }
}

