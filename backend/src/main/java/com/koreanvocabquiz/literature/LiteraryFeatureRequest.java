package com.koreanvocabquiz.literature;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LiteraryFeatureRequest(
        @NotNull Long authorId,
        Long workId,
        @NotNull LiteratureFeatureType type,
        @NotBlank @Size(max = 400) String content
) {}
