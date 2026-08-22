package com.koreanvocabquiz.statistics;

import com.koreanvocabquiz.quiz.QuizMode;

public record ModeLearningStat(
        QuizMode mode,
        int totalQuestionCount,
        int correctCount,
        int incorrectCount,
        int accuracy
) {
    static ModeLearningStat from(QuizMode mode, int totalQuestionCount, int correctCount) {
        int incorrectCount = totalQuestionCount - correctCount;
        int accuracy = totalQuestionCount == 0 ? 0 : Math.round((correctCount * 100.0f) / totalQuestionCount);
        return new ModeLearningStat(mode, totalQuestionCount, correctCount, incorrectCount, accuracy);
    }
}
