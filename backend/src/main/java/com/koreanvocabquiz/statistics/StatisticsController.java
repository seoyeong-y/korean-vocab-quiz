package com.koreanvocabquiz.statistics;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @PostMapping("/quiz-completions")
    public QuizHistoryResponse completeQuiz(@Valid @RequestBody QuizCompletionRequest request) {
        return statisticsService.completeQuiz(request);
    }

    @GetMapping("/dashboard")
    public StatisticsDashboardResponse dashboard() {
        return statisticsService.dashboard();
    }
}
