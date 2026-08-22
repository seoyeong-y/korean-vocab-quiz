package com.koreanvocabquiz.learning;

import com.koreanvocabquiz.vocabulary.Vocabulary;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VocabularyLearningProgressService {

    private final VocabularyLearningProgressRepository learningProgressRepository;

    public VocabularyLearningProgressService(VocabularyLearningProgressRepository learningProgressRepository) {
        this.learningProgressRepository = learningProgressRepository;
    }

    @Transactional
    public void recordAttempt(Vocabulary vocabulary, boolean correct) {
        VocabularyLearningProgress progress = learningProgressRepository.findByVocabulary(vocabulary)
                .orElseGet(() -> new VocabularyLearningProgress(vocabulary));
        progress.recordAttempt(correct);
        learningProgressRepository.save(progress);
    }
}
