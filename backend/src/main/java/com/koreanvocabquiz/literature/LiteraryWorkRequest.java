package com.koreanvocabquiz.literature;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LiteraryWorkRequest(
        @NotNull Long authorId,
        @NotBlank @Size(max = 200) String title
) {}
