package com.koreanvocabquiz.quiz;

import com.koreanvocabquiz.vocabulary.VocabularyCategory;

public record QuizAvailabilityResponse(
        VocabularyCategory category,
        long availableCount
) {
}
