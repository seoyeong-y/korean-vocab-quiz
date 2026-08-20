package com.koreanvocabquiz.quiz;

import jakarta.validation.constraints.NotNull;

public record QuizSubmitRequest(
        @NotNull
        Long vocabularyId,

        @NotNull
        QuizMode mode,

        @NotNull
        Long selectedOptionId,

        boolean wrongAnswerReview
) {
}
