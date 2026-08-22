package com.koreanvocabquiz.learning;

import java.time.LocalDateTime;

import com.koreanvocabquiz.vocabulary.VocabularyCategory;

public record VocabularyLearningProgressResponse(
        Long vocabularyId,
        String word,
        String meaning,
        VocabularyCategory category,
        int attemptCount,
        int correctCount,
        int incorrectCount,
        int accuracy,
        LocalDateTime lastAttemptedAt
) {

    public static VocabularyLearningProgressResponse from(VocabularyLearningProgress progress) {
        int accuracy = progress.getAttemptCount() == 0
                ? 0
                : Math.round((progress.getCorrectCount() * 100.0f) / progress.getAttemptCount());

        return new VocabularyLearningProgressResponse(
                progress.getVocabulary().getId(),
                progress.getVocabulary().getWord(),
                progress.getVocabulary().getMeaning(),
                progress.getVocabulary().getCategory(),
                progress.getAttemptCount(),
                progress.getCorrectCount(),
                progress.getIncorrectCount(),
                accuracy,
                progress.getLastAttemptedAt()
        );
    }
}
