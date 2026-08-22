package com.koreanvocabquiz.quiz;

public record QuizMasteredResponse(
        boolean mastered,
        String correctAnswer,
        Long vocabularyId
) {
}
