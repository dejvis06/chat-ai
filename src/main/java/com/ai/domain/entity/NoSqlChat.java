package com.ai.domain.entity;

import java.time.Instant;

public class NoSqlChat extends Chat {

    private final String name;

    public NoSqlChat(String id, String name, Instant createdAt) {
        super(id, createdAt);
        this.name = name;
    }

    public NoSqlChat(String name, String id) {
        super(id);
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}
