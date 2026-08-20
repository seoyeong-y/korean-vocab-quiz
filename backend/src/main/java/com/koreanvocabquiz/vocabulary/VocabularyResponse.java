package com.koreanvocabquiz.vocabulary;

import java.time.LocalDateTime;

public record VocabularyResponse(
        Long id,
        String word,
        String meaning,
        VocabularyCategory category,
        String exampleSentence,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static VocabularyResponse from(Vocabulary vocabulary) {
        return new VocabularyResponse(
                vocabulary.getId(),
                vocabulary.getWord(),
                vocabulary.getMeaning(),
                vocabulary.getCategory(),
                vocabulary.getExampleSentence(),
                vocabulary.getCreatedAt(),
                vocabulary.getUpdatedAt()
        );
    }
}
