package com.koreanvocabquiz.quiz;

import java.util.List;

public record QuizQuestionResponse(
        String questionId,
        Long vocabularyId,
        QuizMode mode,
        String questionText,
        List<QuizOptionResponse> options
) {
}
