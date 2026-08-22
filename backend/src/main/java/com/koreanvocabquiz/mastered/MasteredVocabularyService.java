package com.koreanvocabquiz.mastered;

import java.util.List;

import com.koreanvocabquiz.quiz.MasteredVocabularyRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MasteredVocabularyService {

    private final MasteredVocabularyRepository masteredVocabularyRepository;

    public MasteredVocabularyService(MasteredVocabularyRepository masteredVocabularyRepository) {
        this.masteredVocabularyRepository = masteredVocabularyRepository;
    }

    public List<MasteredVocabularyResponse> findAll() {
        return masteredVocabularyRepository.findAllByOrderByMasteredAtDesc()
                .stream()
                .map(MasteredVocabularyResponse::from)
                .toList();
    }
}
