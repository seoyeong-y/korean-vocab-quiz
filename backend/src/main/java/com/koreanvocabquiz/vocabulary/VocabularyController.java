package com.koreanvocabquiz.vocabulary;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vocabularies")
public class VocabularyController {

    private final VocabularyService vocabularyService;

    public VocabularyController(VocabularyService vocabularyService) {
        this.vocabularyService = vocabularyService;
    }

    @PostMapping
    public ResponseEntity<VocabularyResponse> create(@Valid @RequestBody VocabularyCreateRequest request) {
        VocabularyResponse response = vocabularyService.create(request);
        return ResponseEntity.created(URI.create("/api/vocabularies/" + response.id()))
                .body(response);
    }

    @GetMapping
    public List<VocabularyResponse> findAll() {
        return vocabularyService.findAll();
    }

    @GetMapping("/{id}")
    public VocabularyResponse findById(@PathVariable Long id) {
        return vocabularyService.findById(id);
    }

    @PutMapping("/{id}")
    public VocabularyResponse update(
            @PathVariable Long id,
            @Valid @RequestBody VocabularyUpdateRequest request
    ) {
        return vocabularyService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        vocabularyService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
