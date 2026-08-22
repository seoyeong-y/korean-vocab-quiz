package com.koreanvocabquiz.quiz;

import com.koreanvocabquiz.vocabulary.VocabularyCategory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record QuizCreateRequest(
        @NotNull
        VocabularyCategory category,

        @NotNull
        QuizMode mode,

        @Min(1)
        int questionCount
) {
}
