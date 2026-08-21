package com.koreanvocabquiz.statistics;

public record LearningCountSummary(
        int totalQuestionCount,
        int correctCount,
        int incorrectCount,
        int accuracy,
        int completedQuizCount
) {
    static LearningCountSummary from(int totalQuestionCount, int correctCount, int completedQuizCount) {
        int incorrectCount = totalQuestionCount - correctCount;
        int accuracy = totalQuestionCount == 0 ? 0 : Math.round((correctCount * 100.0f) / totalQuestionCount);
        return new LearningCountSummary(totalQuestionCount, correctCount, incorrectCount, accuracy, completedQuizCount);
    }
}
