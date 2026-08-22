package com.koreanvocabquiz.literature;

import java.time.LocalDateTime;

public record LiteraryAuthorResponse(Long id, String name, LocalDateTime createdAt, LocalDateTime updatedAt) {
    public static LiteraryAuthorResponse from(LiteraryAuthor author) {
        return new LiteraryAuthorResponse(author.getId(), author.getName(), author.getCreatedAt(), author.getUpdatedAt());
    }
}
