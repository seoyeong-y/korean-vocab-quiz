package com.koreanvocabquiz.literature;

import java.time.LocalDateTime;

public record LiteraryWorkResponse(
        Long id, Long authorId, String authorName, String title, LocalDateTime createdAt, LocalDateTime updatedAt
) {
    public static LiteraryWorkResponse from(LiteraryWork work) {
        return new LiteraryWorkResponse(work.getId(), work.getAuthor().getId(), work.getAuthor().getName(), work.getTitle(), work.getCreatedAt(), work.getUpdatedAt());
    }
}
