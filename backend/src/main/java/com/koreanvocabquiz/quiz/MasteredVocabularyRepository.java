package com.koreanvocabquiz.quiz;

import java.util.Set;

import com.koreanvocabquiz.vocabulary.Vocabulary;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MasteredVocabularyRepository extends JpaRepository<MasteredVocabulary, Long> {

    boolean existsByVocabulary(Vocabulary vocabulary);

    @Query("select mastered.vocabulary.id from MasteredVocabulary mastered")
    Set<Long> findMasteredVocabularyIds();
}
