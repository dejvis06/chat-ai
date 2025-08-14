package com.ai.infrastructure.repository;

import com.ai.domain.model.pagination.ChatPage;
import com.ai.domain.model.pagination.PageMeta;

public interface PagingRepository<ID> {

    /**
     * Retrieves a paginated list of messages for the specified chat.
     *
     * @param id       the unique ID of the chat
     * @param pageMeta pagination details (offset or cursor based)
     * @return a page of chat messages along with pagination metadata
     */
    ChatPage findMessagesByChatId(ID id, PageMeta pageMeta);
}
