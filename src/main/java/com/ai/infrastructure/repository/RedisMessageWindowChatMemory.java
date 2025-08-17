package com.ai.infrastructure.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.util.Assert;

import java.util.List;

public class RedisMessageWindowChatMemory<T, ID> implements ChatMemory<ID> {

    private static final Logger log = LoggerFactory.getLogger(RedisMessageWindowChatMemory.class);

    private static final int DEFAULT_MAX_MESSAGES = 20;
    private final ChatCrudRepository<T, ID> chatCrudRepository;
    private final int maxMessages;
    // TODO inject redis

    private RedisMessageWindowChatMemory(ChatCrudRepository<T, ID> chatCrudRepository, int maxMessages) {
        Assert.notNull(chatCrudRepository, "chatCrudRepository cannot be null");
        Assert.isTrue(maxMessages > 0, "maxMessages must be greater than 0");
        this.chatCrudRepository = chatCrudRepository;
        this.maxMessages = maxMessages;
    }

    @Override
    public void add(ID conversationId, List<Message> messages) {
        Assert.notNull(conversationId, "conversationId cannot be null ");
        Assert.notNull(messages, "messages cannot be null");
        Assert.noNullElements(messages, "messages cannot contain null elements");

        // TODO add redis handling
        this.chatCrudRepository.saveAll(conversationId, messages);
    }

    @Override
    public List<Message> get(ID conversationId) {
        Assert.notNull(conversationId, "conversationId cannot be null");
        // TODO replace with: get from redis
        return this.chatCrudRepository.findLastNByConversationId(conversationId, maxMessages);
    }

    @Override
    public void clear(ID conversationId) {
        Assert.notNull(conversationId, "conversationId cannot be null or empty");
        this.chatCrudRepository.deleteByConversationId(conversationId);
    }

    public static final class Builder<T, ID> {
        private ChatCrudRepository<T, ID> chatCrudRepository;
        private int maxMessages = 20;

        private Builder() {
        }

        public Builder<T, ID> chatCrudRepository(ChatCrudRepository<T, ID> chatCrudRepository) {
            this.chatCrudRepository = chatCrudRepository;
            return this;
        }

        public Builder<T, ID> maxMessages(int maxMessages) {
            this.maxMessages = maxMessages;
            return this;
        }

        public RedisMessageWindowChatMemory<T, ID> build() {
            if (this.chatCrudRepository == null) {
                throw new IllegalStateException("ChatCrudRepository must not be null");
            }

            return new RedisMessageWindowChatMemory(this.chatCrudRepository, this.maxMessages);
        }
    }
}
