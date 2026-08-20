package com.koreanvocabquiz.wronganswer;

import java.util.List;

import com.koreanvocabquiz.quiz.QuizQuestionResponse;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wrong-answers")
public class WrongAnswerController {

    private final WrongAnswerService wrongAnswerService;

    public WrongAnswerController(WrongAnswerService wrongAnswerService) {
        this.wrongAnswerService = wrongAnswerService;
    }

    @GetMapping
    public List<WrongAnswerResponse> findAll() {
        return wrongAnswerService.findAll();
    }

    @PostMapping("/quizzes")
    public List<QuizQuestionResponse> createReviewQuiz(@Valid @RequestBody WrongAnswerQuizCreateRequest request) {
        return wrongAnswerService.createReviewQuiz(request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        wrongAnswerService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAll() {
        wrongAnswerService.deleteAll();
        return ResponseEntity.noContent().build();
    }
}
