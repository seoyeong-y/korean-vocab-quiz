package com.koreanvocabquiz.wronganswer;

import com.koreanvocabquiz.quiz.QuizMode;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record WrongAnswerQuizCreateRequest(
        @NotNull
        QuizMode mode,

        @Min(1)
        Integer questionCount
) {
}
