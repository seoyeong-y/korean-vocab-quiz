package com.koreanvocabquiz.vocabulary;

public record VocabularyImageFile(
        int imageNumber,
        String mediaType,
        byte[] content
) {
}
