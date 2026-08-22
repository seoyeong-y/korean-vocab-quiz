package com.koreanvocabquiz.statistics;

import java.util.List;

import com.koreanvocabquiz.quiz.QuizMode;
import com.koreanvocabquiz.vocabulary.VocabularyCategory;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record QuizCompletionRequest(
        @NotNull
        VocabularyCategory category,

        @NotNull
        QuizMode mode,

        @NotEmpty
        List<String> questionIds,

        boolean wrongAnswerReview
) {
}
