package com.ai.domain.entity;

import java.time.Instant;

public class SqlChat extends Chat {

    private String name;

    public SqlChat(String id, String name, Instant createdAt) {
        super(id, createdAt);
        this.name = name;
    }

    public SqlChat(String name) {
        super(null);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
