package com.koreanvocabquiz.statistics;

import java.util.List;

import com.koreanvocabquiz.learning.VocabularyLearningProgressResponse;

public record StatisticsDashboardResponse(
        LearningCountSummary total,
        LearningCountSummary today,
        List<CategoryLearningStat> categories,
        List<ModeLearningStat> modes,
        List<QuizHistoryResponse> recentHistories,
        List<MostWrongVocabularyResponse> mostWrongVocabularies,
        List<VocabularyLearningProgressResponse> vocabularyProgresses
) {
}
