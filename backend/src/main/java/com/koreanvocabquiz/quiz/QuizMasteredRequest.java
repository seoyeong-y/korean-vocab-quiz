package com.koreanvocabquiz.quiz;

import jakarta.validation.constraints.NotBlank;

public record QuizMasteredRequest(
        @NotBlank
        String questionId
) {
}
