package com.ai.infrastructure.repository;

import org.springframework.ai.chat.messages.Message;

import java.util.List;

public interface ChatCrudRepository<T> extends ListChatCrudRepository<T>, PagingRepository {

    T save(String chatName);

    void deleteById(String id);

    void deleteByConversationId(String id);

    List<Message> findByConversationId(String id);

    List<Message> findLastNByConversationId(String id, int limit);
}
