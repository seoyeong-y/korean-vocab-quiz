package com.koreanvocabquiz.literature;

import java.time.LocalDateTime;
import java.util.Map;

public record LiteraryQuizQuestionSession(
        String questionId,
        LiteratureQuizType quizType,
        Map<String, String> optionAnswers,
        String correctOptionId,
        String correctAnswer,
        LocalDateTime expiresAt
) {
    public boolean isExpired(LocalDateTime now) { return !expiresAt.isAfter(now); }
}
