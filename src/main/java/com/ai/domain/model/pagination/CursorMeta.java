package com.ai.domain.model.pagination;

public record CursorMeta(String nextCursor, int pageSize) implements PageMeta {
}