package com.koreanvocabquiz.statistics;

import com.koreanvocabquiz.vocabulary.VocabularyCategory;

public record CategoryLearningStat(
        VocabularyCategory category,
        int totalQuestionCount,
        int correctCount,
        int incorrectCount,
        int accuracy
) {
    static CategoryLearningStat from(VocabularyCategory category, int totalQuestionCount, int correctCount) {
        int incorrectCount = totalQuestionCount - correctCount;
        int accuracy = totalQuestionCount == 0 ? 0 : Math.round((correctCount * 100.0f) / totalQuestionCount);
        return new CategoryLearningStat(category, totalQuestionCount, correctCount, incorrectCount, accuracy);
    }
}
