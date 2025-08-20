package com.ai.infrastructure.metadata;

import java.util.Map;

public interface MessageMetadataAppender {

    Map<String, Object> appendMetadata(Map<String, Object> baseMetadata);
}
