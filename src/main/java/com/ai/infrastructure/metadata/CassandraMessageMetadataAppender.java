package com.ai.infrastructure.metadata;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
@ConditionalOnProperty(
        value = "app.cassandra.enabled",
        havingValue = "true"
)
public class CassandraMessageMetadataAppender implements MessageMetadataAppender {

    @Override
    public Map<String, Object> appendMetadata(Map<String, Object> baseMetadata) {
        Map<String, Object> result = new HashMap<>(baseMetadata);
        result.put("msg_timestamp", Instant.now());
        return result;
    }
}
