package com.koreanvocabquiz.literature;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LiteraryAuthorRequest(@NotBlank @Size(max = 100) String name) {}
