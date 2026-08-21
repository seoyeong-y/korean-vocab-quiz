package com.koreanvocabquiz.statistics;

import java.util.List;

public record StatisticsDashboardResponse(
        LearningCountSummary total,
        LearningCountSummary today,
        List<CategoryLearningStat> categories,
        List<ModeLearningStat> modes,
        List<QuizHistoryResponse> recentHistories,
        List<MostWrongVocabularyResponse> mostWrongVocabularies
) {
}
