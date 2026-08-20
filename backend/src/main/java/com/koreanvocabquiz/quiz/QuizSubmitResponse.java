package com.koreanvocabquiz.quiz;

public record QuizSubmitResponse(
        boolean correct,
        String correctAnswer,
        Long vocabularyId
) {
}
