package com.ai.domain.entity;

import java.time.Instant;

public abstract class Chat {

    private String id;
    private Instant createdAt;

    public Chat() {
    }

    public Chat(String id) {
        this.id = id;
        this.createdAt = Instant.now();
    }

    public Chat(String id, Instant createdAt) {
        this.id = id;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
