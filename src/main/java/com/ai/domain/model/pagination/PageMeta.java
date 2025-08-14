package com.ai.domain.model.pagination;

public sealed interface PageMeta permits OffsetMeta, CursorMeta {}