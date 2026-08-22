package com.koreanvocabquiz.literature;

import java.time.LocalDateTime;

public record LiteraryFeatureResponse(
        Long id, Long authorId, String authorName, Long workId, String workTitle,
        LiteratureFeatureType type, String content, LocalDateTime createdAt, LocalDateTime updatedAt
) {
    public static LiteraryFeatureResponse from(LiteraryFeature feature) {
        return new LiteraryFeatureResponse(
                feature.getId(), feature.getAuthor().getId(), feature.getAuthor().getName(),
                feature.getWork() == null ? null : feature.getWork().getId(),
                feature.getWork() == null ? null : feature.getWork().getTitle(),
                feature.getType(), feature.getContent(), feature.getCreatedAt(), feature.getUpdatedAt()
        );
    }
}
