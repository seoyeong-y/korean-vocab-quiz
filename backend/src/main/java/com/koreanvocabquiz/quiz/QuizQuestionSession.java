package com.koreanvocabquiz.quiz;

import java.time.LocalDateTime;
import java.util.Map;

public record QuizQuestionSession(
        String questionId,
        Long vocabularyId,
        QuizMode mode,
        Map<String, Long> optionVocabularyIds,
        String correctOptionId,
        String correctAnswer,
        LocalDateTime expiresAt
) {
    public boolean isExpired(LocalDateTime now) {
        return !expiresAt.isAfter(now);
    }
}
