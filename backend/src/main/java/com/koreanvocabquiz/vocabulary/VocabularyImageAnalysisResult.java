package com.koreanvocabquiz.vocabulary;

public record VocabularyImageAnalysisResult(
        int imageNumber,
        String word,
        String meaning,
        String category,
        boolean needsReview,
        Double confidence
) {
}
