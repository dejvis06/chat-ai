package com.ai.domain.entity;

import java.time.Instant;

public class NoSqlChat extends Chat {

    public NoSqlChat(String id, Instant createdAt) {
        super(id, createdAt);
    }

    public NoSqlChat(String id) {
        super(id);
    }
}
