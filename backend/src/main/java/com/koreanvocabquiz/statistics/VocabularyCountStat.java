package com.koreanvocabquiz.statistics;

import com.koreanvocabquiz.vocabulary.VocabularyCategory;

public record VocabularyCountStat(
        VocabularyCategory category,
        long totalCount,
        long attemptedCount
) {
}
