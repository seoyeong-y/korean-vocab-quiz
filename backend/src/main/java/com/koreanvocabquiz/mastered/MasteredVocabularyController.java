package com.koreanvocabquiz.mastered;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mastered-vocabularies")
public class MasteredVocabularyController {

    private final MasteredVocabularyService masteredVocabularyService;

    public MasteredVocabularyController(MasteredVocabularyService masteredVocabularyService) {
        this.masteredVocabularyService = masteredVocabularyService;
    }

    @GetMapping
    public List<MasteredVocabularyResponse> findAll() {
        return masteredVocabularyService.findAll();
    }
}
