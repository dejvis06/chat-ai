package com.ai.infrastructure.repository;

import org.springframework.ai.chat.messages.Message;

import java.util.List;

public interface ChatCrudRepository<T, ID> extends ListChatCrudRepository<T, ID>, PagingRepository<ID> {

    T save(String chatName);

    void deleteById(ID id);

    void deleteByConversationId(ID id);

    List<Message> findByConversationId(ID id);

    List<Message> findLastNByConversationId(ID id, int limit);
}
