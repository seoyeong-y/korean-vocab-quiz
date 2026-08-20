package com.koreanvocabquiz.quiz;

import jakarta.validation.constraints.NotBlank;

public record QuizSubmitRequest(
        @NotBlank
        String questionId,

        @NotBlank
        String selectedOptionId,

        boolean wrongAnswerReview
) {
}
