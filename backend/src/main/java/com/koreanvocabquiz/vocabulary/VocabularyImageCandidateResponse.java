package com.koreanvocabquiz.vocabulary;

public record VocabularyImageCandidateResponse(
        int imageNumber,
        int rowNumber,
        String word,
        String meaning,
        VocabularyCategory category,
        boolean needsReview,
        Double confidence
) {
}
