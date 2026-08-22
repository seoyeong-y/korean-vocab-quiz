package com.koreanvocabquiz.literature;

import jakarta.validation.constraints.NotBlank;

public record LiteraryQuizSubmitRequest(@NotBlank String selectedOptionId) {}
