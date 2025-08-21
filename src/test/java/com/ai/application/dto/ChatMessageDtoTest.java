package com.ai.application.dto;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import static org.assertj.core.api.Assertions.assertThat;

class ChatMessageDtoTest {

    @Test
    void from_shouldMapUserMessage() {
        UserMessage msg = new UserMessage("hello");
        ChatMessageDto dto = ChatMessageDto.from(msg);

        assertThat(dto.role()).isEqualTo("user");
        assertThat(dto.content()).isEqualTo("hello");
    }

    @Test
    void from_shouldMapAssistantMessage() {
        AssistantMessage msg = new AssistantMessage("hi there");
        ChatMessageDto dto = ChatMessageDto.from(msg);

        assertThat(dto.role()).isEqualTo("assistant");
        assertThat(dto.content()).isEqualTo("hi there");
    }

    @Test
    void from_shouldMapSystemMessage() {
        SystemMessage msg = new SystemMessage("system info");
        ChatMessageDto dto = ChatMessageDto.from(msg);

        assertThat(dto.role()).isEqualTo("system");
        assertThat(dto.content()).isEqualTo("system info");
    }
}
