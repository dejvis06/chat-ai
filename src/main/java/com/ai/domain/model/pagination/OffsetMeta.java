package com.ai.domain.model.pagination;

public record OffsetMeta(Integer nextPage, int pageSize, boolean hasNext) implements PageMeta {
}