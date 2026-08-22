package com.koreanvocabquiz.literature;

import java.util.List;

public record LiteraryQuizQuestionResponse(
        String questionId,
        LiteratureQuizType quizType,
        String authorName,
        List<String> workTitles,
        String feature,
        String questionText,
        List<LiteraryQuizOptionResponse> options
) {}
