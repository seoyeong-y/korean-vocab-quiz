package com.koreanvocabquiz.quiz;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/quizzes")
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @PostMapping
    public List<QuizQuestionResponse> create(@Valid @RequestBody QuizCreateRequest request) {
        return quizService.create(request);
    }

    @PostMapping("/submit")
    public QuizSubmitResponse submit(@Valid @RequestBody QuizSubmitRequest request) {
        return quizService.submit(request);
    }

    @PostMapping("/mastered")
    public QuizMasteredResponse markMastered(@Valid @RequestBody QuizMasteredRequest request) {
        return quizService.markMastered(request);
    }

    @DeleteMapping("/mastered/{questionId}")
    public QuizMasteredResponse unmarkMastered(@PathVariable String questionId) {
        return quizService.unmarkMastered(questionId);
    }
}
