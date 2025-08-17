package com.ai.infrastructure.repository;

import org.springframework.ai.chat.messages.Message;
import org.springframework.util.Assert;

import java.util.List;

public interface ChatMemory<ID> {

    default void add(ID conversationId, Message message) {
        Assert.notNull(conversationId, "conversationId cannot be null");
        Assert.notNull(message, "message cannot be null");
        this.add(conversationId, List.of(message));
    }

    void add(ID conversationId, List<Message> messages);

    List<Message> get(ID conversationId);

    void clear(ID conversationId);
}
