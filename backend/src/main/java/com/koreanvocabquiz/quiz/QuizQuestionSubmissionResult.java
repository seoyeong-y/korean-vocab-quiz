package com.koreanvocabquiz.quiz;

import com.koreanvocabquiz.vocabulary.VocabularyCategory;

public record QuizQuestionSubmissionResult(
        String questionId,
        Long vocabularyId,
        VocabularyCategory category,
        QuizMode mode,
        boolean correct
) {
}
