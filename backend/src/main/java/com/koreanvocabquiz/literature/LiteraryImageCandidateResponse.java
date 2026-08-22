package com.koreanvocabquiz.literature;

public record LiteraryImageCandidateResponse(
        int imageNumber,
        int rowNumber,
        String author,
        String work,
        String feature,
        String featureType,
        String status,
        String reason,
        boolean needsReview
) {}
