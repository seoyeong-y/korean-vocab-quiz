package com.koreanvocabquiz.vocabulary;

public record VocabularyBatchItemRequest(
        String word,
        String meaning,
        String category
) {
}
