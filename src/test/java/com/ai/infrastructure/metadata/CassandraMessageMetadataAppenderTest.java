package com.ai.infrastructure.metadata;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CassandraMessageMetadataAppenderTest {

    @Test
    void appendMetadata_shouldPreserveAndAddTimestamp() {
        CassandraMessageMetadataAppender appender = new CassandraMessageMetadataAppender();

        Map<String, Object> input = Map.of("key1", "value1");
        Map<String, Object> result = appender.appendMetadata(input);

        // existing metadata preserved
        assertThat(result).containsEntry("key1", "value1");

        // timestamp added
        assertThat(result).containsKey("msg_timestamp");
        assertThat(result.get("msg_timestamp")).isInstanceOf(Instant.class);
    }
}
