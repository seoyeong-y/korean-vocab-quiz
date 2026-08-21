package com.koreanvocabquiz.statistics;

import com.koreanvocabquiz.vocabulary.VocabularyCategory;
import com.koreanvocabquiz.wronganswer.WrongAnswer;

public record MostWrongVocabularyResponse(
        Long vocabularyId,
        String word,
        String meaning,
        VocabularyCategory category,
        int wrongCount
) {
    static MostWrongVocabularyResponse from(WrongAnswer wrongAnswer) {
        return new MostWrongVocabularyResponse(
                wrongAnswer.getVocabulary().getId(),
                wrongAnswer.getVocabulary().getWord(),
                wrongAnswer.getVocabulary().getMeaning(),
                wrongAnswer.getVocabulary().getCategory(),
                wrongAnswer.getWrongCount()
        );
    }
}
