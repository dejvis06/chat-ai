package com.ai.application.domain.model.pagination;

import com.ai.domain.model.pagination.CursorMeta;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CursorMetaTest {

    @Test
    void constructor_andGetters_shouldWork() {
        CursorMeta meta = new CursorMeta("cursor123", 10);

        assertThat(meta.nextCursor()).isEqualTo("cursor123");
        assertThat(meta.pageSize()).isEqualTo(10);
    }
}
