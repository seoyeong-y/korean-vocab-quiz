package com.koreanvocabquiz.vocabulary;

import java.util.List;

import com.koreanvocabquiz.common.ResourceNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class VocabularyService {

    private final VocabularyRepository vocabularyRepository;

    public VocabularyService(VocabularyRepository vocabularyRepository) {
        this.vocabularyRepository = vocabularyRepository;
    }

    @Transactional
    public VocabularyResponse create(VocabularyCreateRequest request) {
        Vocabulary vocabulary = new Vocabulary(
                request.word(),
                request.meaning(),
                request.exampleSentence()
        );

        return VocabularyResponse.from(vocabularyRepository.save(vocabulary));
    }

    public List<VocabularyResponse> findAll() {
        return vocabularyRepository.findAll()
                .stream()
                .map(VocabularyResponse::from)
                .toList();
    }

    public VocabularyResponse findById(Long id) {
        return VocabularyResponse.from(getVocabulary(id));
    }

    @Transactional
    public VocabularyResponse update(Long id, VocabularyUpdateRequest request) {
        Vocabulary vocabulary = getVocabulary(id);
        vocabulary.update(
                request.word(),
                request.meaning(),
                request.exampleSentence()
        );

        return VocabularyResponse.from(vocabulary);
    }

    @Transactional
    public void delete(Long id) {
        Vocabulary vocabulary = getVocabulary(id);
        vocabularyRepository.delete(vocabulary);
    }

    private Vocabulary getVocabulary(Long id) {
        return vocabularyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vocabulary not found. id=" + id));
    }
}
