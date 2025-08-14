package com.ai.domain.model.pagination;

import com.ai.application.dto.ChatMessageDto;

import java.util.List;

public record ChatPage(List<ChatMessageDto> messages, PageMeta pageMeta) {
}
