package com.koreanvocabquiz.mastered;

import java.time.LocalDateTime;

import com.koreanvocabquiz.quiz.MasteredVocabulary;
import com.koreanvocabquiz.vocabulary.VocabularyCategory;

public record MasteredVocabularyResponse(
        Long id,
        Long vocabularyId,
        String word,
        String meaning,
        VocabularyCategory category,
        LocalDateTime masteredAt
) {
    public static MasteredVocabularyResponse from(MasteredVocabulary masteredVocabulary) {
        return new MasteredVocabularyResponse(
                masteredVocabulary.getId(),
                masteredVocabulary.getVocabulary().getId(),
                masteredVocabulary.getVocabulary().getWord(),
                masteredVocabulary.getVocabulary().getMeaning(),
                masteredVocabulary.getVocabulary().getCategory(),
                masteredVocabulary.getMasteredAt()
        );
    }
}
