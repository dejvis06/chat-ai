package com.ai.infrastructure.repository;

import com.ai.domain.entity.Chat;

public interface ChatRepository<T extends Chat> extends ChatCrudRepository<T> {
}
