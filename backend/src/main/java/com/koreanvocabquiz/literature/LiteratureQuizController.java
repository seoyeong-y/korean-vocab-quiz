package com.koreanvocabquiz.literature;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/literature/quizzes")
public class LiteratureQuizController {
    private final LiteratureQuizService service;
    public LiteratureQuizController(LiteratureQuizService service) { this.service = service; }
    @GetMapping("/availability")
    public List<LiteraryQuizAvailabilityResponse> availability() { return service.availability(); }
    @PostMapping
    public List<LiteraryQuizQuestionResponse> create(@Valid @RequestBody LiteraryQuizCreateRequest request) { return service.create(request); }
    @PostMapping("/{questionId}/submit")
    public LiteraryQuizSubmitResponse submit(@PathVariable String questionId, @Valid @RequestBody LiteraryQuizSubmitRequest request) { return service.submit(questionId, request); }
}
