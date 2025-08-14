package com.ai.infrastructure.repository;

import com.ai.domain.entity.NoSqlChat;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.util.Assert;

import java.util.List;

public abstract class ChatRepository<T, ID> implements ChatMemory, PagingRepository<ID> {

    private static final int DEFAULT_MAX_MESSAGES = 20;
    private final ChatMemoryRepository chatMemoryRepository;
    private final int maxMessages;

    protected ChatRepository(ChatMemoryRepository chatMemoryRepository, int maxMessages) {
        Assert.notNull(chatMemoryRepository, "chatMemoryRepository cannot be null");
        Assert.isTrue(maxMessages > 0, "maxMessages must be greater than 0");
        this.chatMemoryRepository = chatMemoryRepository;
        this.maxMessages = maxMessages;
    }

    /**
     * Saves a chat with the given name to the data store.
     *
     * @param chatName the name of the chat
     * @return the saved chat instance
     */
    public abstract T save(String chatName);

    /**
     * Retrieves all chats from the database.
     *
     * @return a {@link List} containing every {@link NoSqlChat} stored in the database
     */
    public abstract List<T> findAll();

    /**
     * Adds the given messages to the specified conversation.
     *
     * @param conversationId the unique ID of the conversation
     * @param messages       the list of messages to add
     */
    @Override
    public void add(String conversationId, List<Message> messages) {
        Assert.hasText(conversationId, "conversationId cannot be null or empty");
        Assert.notNull(messages, "messages cannot be null");
        Assert.noNullElements(messages, "messages cannot contain null elements");

        this.chatMemoryRepository.saveAll(conversationId, messages);
    }

    /**
     * Retrieves the maximum number of recent messages for the given conversation.
     * Used to provide context awareness for the AI.
     *
     * @param conversationId the unique ID of the conversation
     * @return a list of the most recent messages
     */
    public abstract List<Message> getMaxMessages(String conversationId);

    /**
     * Retrieves all messages for the specified conversation.
     *
     * @param conversationId the unique ID of the conversation
     * @return a list of messages in the conversation
     */
    public List<Message> get(String conversationId) {
        Assert.hasText(conversationId, "conversationId cannot be null or empty");
        return this.chatMemoryRepository.findByConversationId(conversationId);
    }

    /**
     * Removes all messages for the specified conversation.
     *
     * @param conversationId the unique ID of the conversation
     */
    public void clear(String conversationId) {
        Assert.hasText(conversationId, "conversationId cannot be null or empty");
        this.chatMemoryRepository.deleteByConversationId(conversationId);
    }
}
