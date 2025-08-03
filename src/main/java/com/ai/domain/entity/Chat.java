package com.ai.domain.entity;

import java.time.Instant;

public class Chat {

    private String id;        // same as chatId used by ChatMemory
    private String name;
    private Instant createdAt;

    public Chat() {}

    public Chat(String name) {
        this.name = name;
        this.createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
