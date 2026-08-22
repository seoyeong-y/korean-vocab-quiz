package com.koreanvocabquiz.literature;

public record LiteraryCsvRowRequest(
        Integer rowNumber,
        boolean selected,
        String author,
        String work,
        String feature,
        LiteratureFeatureType featureType
) {}
