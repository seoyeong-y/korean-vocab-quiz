package com.koreanvocabquiz.vocabulary;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VocabularyRepository extends JpaRepository<Vocabulary, Long> {

    boolean existsByWordAndMeaningAndCategory(String word, String meaning, VocabularyCategory category);

    List<Vocabulary> findByCategory(VocabularyCategory category);

    List<Vocabulary> findByCategoryIn(Collection<VocabularyCategory> categories);
}
