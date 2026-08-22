package com.koreanvocabquiz.wronganswer;

import com.koreanvocabquiz.quiz.QuizMode;
import com.koreanvocabquiz.vocabulary.VocabularyCategory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record WrongAnswerQuizCreateRequest(
        @NotNull
        QuizMode mode,

        VocabularyCategory category,

        @Min(1)
        Integer questionCount
) {
}
