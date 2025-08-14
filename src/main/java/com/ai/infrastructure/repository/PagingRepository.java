package com.ai.infrastructure.repository;

import com.ai.domain.model.pagination.ChatPage;
import com.ai.domain.model.pagination.PageMeta;

public interface PagingRepository<ID> {
    ChatPage findAll(ID id, PageMeta pageMeta);
}
