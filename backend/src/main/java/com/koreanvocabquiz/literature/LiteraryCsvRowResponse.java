package com.koreanvocabquiz.literature;

public record LiteraryCsvRowResponse(
        Integer rowNumber,
        String author,
        String work,
        String feature,
        LiteratureFeatureType featureType,
        String status,
        String reason
) {}
