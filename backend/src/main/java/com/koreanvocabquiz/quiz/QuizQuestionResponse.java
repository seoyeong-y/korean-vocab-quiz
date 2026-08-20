package com.koreanvocabquiz.quiz;

import java.util.List;

public record QuizQuestionResponse(
        Long vocabularyId,
        QuizMode mode,
        String questionText,
        List<QuizOptionResponse> options
) {
}
