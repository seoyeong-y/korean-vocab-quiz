package com.koreanvocabquiz.wronganswer;

import java.time.LocalDateTime;

import com.koreanvocabquiz.quiz.QuizMode;
import com.koreanvocabquiz.vocabulary.VocabularyCategory;

public record WrongAnswerResponse(
        Long id,
        Long vocabularyId,
        String word,
        String meaning,
        VocabularyCategory category,
        QuizMode quizMode,
        int wrongCount,
        LocalDateTime lastWrongAt
) {
    public static WrongAnswerResponse from(WrongAnswer wrongAnswer) {
        return new WrongAnswerResponse(
                wrongAnswer.getId(),
                wrongAnswer.getVocabulary().getId(),
                wrongAnswer.getVocabulary().getWord(),
                wrongAnswer.getVocabulary().getMeaning(),
                wrongAnswer.getVocabulary().getCategory(),
                wrongAnswer.getQuizMode(),
                wrongAnswer.getWrongCount(),
                wrongAnswer.getLastWrongAt()
        );
    }
}
