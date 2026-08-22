package com.koreanvocabquiz.learning;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.koreanvocabquiz.vocabulary.Vocabulary;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface VocabularyLearningProgressRepository extends JpaRepository<VocabularyLearningProgress, Long> {

    Optional<VocabularyLearningProgress> findByVocabulary(Vocabulary vocabulary);

    List<VocabularyLearningProgress> findByVocabularyIdIn(Collection<Long> vocabularyIds);

    @Query("select progress.vocabulary.id from VocabularyLearningProgress progress")
    Set<Long> findVocabularyIds();

    @EntityGraph(attributePaths = "vocabulary")
    List<VocabularyLearningProgress> findTop20ByOrderByLastAttemptedAtDesc();
}
