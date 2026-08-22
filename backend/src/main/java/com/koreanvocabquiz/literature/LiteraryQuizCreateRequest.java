package com.koreanvocabquiz.literature;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record LiteraryQuizCreateRequest(
        @NotNull LiteratureQuizType quizType,
        @Min(1) @Max(100) int questionCount
) {}
