package com.koreanvocabquiz.vocabulary;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VocabularyCreateRequest(
        @NotBlank
        @Size(max = 100)
        String word,

        @NotBlank
        @Size(max = 500)
        String meaning,

        @Size(max = 1000)
        String exampleSentence
) {
}
