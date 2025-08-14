package com.ai.infrastructure.repository;

import com.ai.domain.model.pagination.ChatPage;
import com.ai.domain.model.pagination.PageMeta;

public interface PagingRepository<ID> {

    ChatPage findMessagesByChatId(ID id, PageMeta pageMeta);
}
