package com.koreanvocabquiz.quiz;

import jakarta.validation.constraints.NotBlank;

public record QuizSubmitRequest(
        @NotBlank
        String questionId,

        String selectedOptionId,

        String selectedAnswer,

        boolean wrongAnswerReview
) {
}
