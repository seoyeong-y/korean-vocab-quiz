package com.koreanvocabquiz.statistics;

import java.time.LocalDateTime;

import com.koreanvocabquiz.quiz.QuizMode;
import com.koreanvocabquiz.vocabulary.VocabularyCategory;

public record QuizHistoryResponse(
        Long id,
        LocalDateTime completedAt,
        VocabularyCategory category,
        QuizMode quizMode,
        int totalCount,
        int correctCount,
        int incorrectCount,
        int accuracy
) {
    static QuizHistoryResponse from(QuizHistory history) {
        int accuracy = history.getTotalCount() == 0
                ? 0
                : Math.round((history.getCorrectCount() * 100.0f) / history.getTotalCount());
        return new QuizHistoryResponse(
                history.getId(),
                history.getCompletedAt(),
                history.getCategory(),
                history.getQuizMode(),
                history.getTotalCount(),
                history.getCorrectCount(),
                history.getIncorrectCount(),
                accuracy
        );
    }
}
