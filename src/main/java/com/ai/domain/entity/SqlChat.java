package com.ai.domain.entity;

import java.time.Instant;

public class SqlChat extends Chat {

    private String name;

    public SqlChat() {
    }

    public SqlChat(String name) {
        this.name = name;
        setCreatedAt(Instant.now());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
