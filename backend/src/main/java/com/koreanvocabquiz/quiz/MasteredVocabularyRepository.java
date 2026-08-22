package com.koreanvocabquiz.quiz;

import java.util.List;
import java.util.Set;

import com.koreanvocabquiz.vocabulary.Vocabulary;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MasteredVocabularyRepository extends JpaRepository<MasteredVocabulary, Long> {

    boolean existsByVocabulary(Vocabulary vocabulary);

    void deleteByVocabulary(Vocabulary vocabulary);

    @EntityGraph(attributePaths = "vocabulary")
    List<MasteredVocabulary> findAllByOrderByMasteredAtDesc();

    @Query("select mastered.vocabulary.id from MasteredVocabulary mastered")
    Set<Long> findMasteredVocabularyIds();
}
