package com.ai.infrastructure.repository;

import org.springframework.ai.chat.messages.Message;

import java.util.List;

public interface ListChatCrudRepository<T> {

    void saveAll(String id, List<Message> messages);

    List<T> findAll();

    List<String> findConversationIds();
}
