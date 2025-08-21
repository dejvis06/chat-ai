package com.ai.application.domain.model.pagination;

import com.ai.application.dto.ChatMessageDto;
import com.ai.domain.model.pagination.ChatPage;
import com.ai.domain.model.pagination.CursorMeta;
import com.ai.domain.model.pagination.PageMeta;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChatPageTest {

    @Test
    void constructor_andGetters_shouldWork() {
        ChatMessageDto msg1 = new ChatMessageDto("user", "Hello");
        ChatMessageDto msg2 = new ChatMessageDto("assistant", "Hi there!");
        PageMeta pageMeta = new CursorMeta("cursor123", 5);

        ChatPage page = new ChatPage(List.of(msg1, msg2), pageMeta);

        assertThat(page.messages()).containsExactly(msg1, msg2);
        assertThat(page.pageMeta()).isEqualTo(pageMeta);
    }
}
