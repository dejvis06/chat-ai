package com.ai.infrastructure.repository;

import com.ai.domain.entity.Chat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.util.Assert;

import java.util.List;

public class RedisMessageWindowChatMemory<T extends Chat> implements ChatMemory {

    private static final Logger log = LoggerFactory.getLogger(RedisMessageWindowChatMemory.class);

    private static final int DEFAULT_MAX_MESSAGES = 20;
    private final ChatRepository<T> chatRepository;
    private final int maxMessages;
    // TODO inject redis

    private RedisMessageWindowChatMemory(ChatRepository<T> chatRepository, int maxMessages) {
        Assert.notNull(chatRepository, "chatRepository cannot be null");
        Assert.isTrue(maxMessages > 0, "maxMessages must be greater than 0");
        this.chatRepository = chatRepository;
        this.maxMessages = maxMessages;
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        Assert.notNull(conversationId, "conversationId cannot be null ");
        Assert.notNull(messages, "messages cannot be null");
        Assert.noNullElements(messages, "messages cannot contain null elements");

        // TODO add redis handling
        this.chatRepository.saveAll(conversationId, messages);
    }

    @Override
    public List<Message> get(String conversationId) {
        Assert.notNull(conversationId, "conversationId cannot be null");
        // TODO replace with: get from redis
        return this.chatRepository.findLastNByConversationId(conversationId, maxMessages);
    }

    @Override
    public void clear(String conversationId) {
        Assert.notNull(conversationId, "conversationId cannot be null or empty");
        this.chatRepository.deleteById(conversationId);
    }

    public static <T extends Chat> Builder<T> builder() {
        return new RedisMessageWindowChatMemory.Builder<>();
    }

    public static final class Builder<T extends Chat> {
        private ChatRepository<T> chatRepository;
        private int maxMessages = DEFAULT_MAX_MESSAGES;

        private Builder() {
        }

        public Builder<T> chatRepository(ChatRepository<T> chatRepository) {
            this.chatRepository = chatRepository;
            return this;
        }

        public Builder<T> maxMessages(int maxMessages) {
            this.maxMessages = maxMessages;
            return this;
        }

        public RedisMessageWindowChatMemory build() {
            if (this.chatRepository == null) {
                throw new IllegalStateException("chatRepository must not be null");
            }

            return new RedisMessageWindowChatMemory(this.chatRepository, this.maxMessages);
        }
    }
}
