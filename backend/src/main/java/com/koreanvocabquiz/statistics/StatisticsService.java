package com.koreanvocabquiz.statistics;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.koreanvocabquiz.learning.VocabularyLearningProgressRepository;
import com.koreanvocabquiz.learning.VocabularyLearningProgressResponse;
import com.koreanvocabquiz.quiz.QuizMode;
import com.koreanvocabquiz.quiz.QuizQuestionSessionStore;
import com.koreanvocabquiz.quiz.QuizQuestionSubmissionResult;
import com.koreanvocabquiz.vocabulary.VocabularyCategory;
import com.koreanvocabquiz.vocabulary.VocabularyRepository;
import com.koreanvocabquiz.wronganswer.WrongAnswerRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StatisticsService {

    private static final List<VocabularyCategory> DASHBOARD_CATEGORIES = List.of(
            VocabularyCategory.NATIVE_KOREAN,
            VocabularyCategory.SINO_KOREAN,
            VocabularyCategory.LOANWORD,
            VocabularyCategory.PROVERB,
            VocabularyCategory.IDIOM,
            VocabularyCategory.FOUR_CHARACTER_IDIOM
    );
    private static final List<QuizMode> DASHBOARD_MODES = List.of(
            QuizMode.WORD_TO_MEANING,
            QuizMode.MEANING_TO_WORD,
            QuizMode.MIXED
    );

    private final QuizHistoryRepository quizHistoryRepository;
    private final QuizQuestionSessionStore sessionStore;
    private final WrongAnswerRepository wrongAnswerRepository;
    private final VocabularyLearningProgressRepository learningProgressRepository;
    private final VocabularyRepository vocabularyRepository;

    public StatisticsService(
            QuizHistoryRepository quizHistoryRepository,
            QuizQuestionSessionStore sessionStore,
            WrongAnswerRepository wrongAnswerRepository,
            VocabularyLearningProgressRepository learningProgressRepository,
            VocabularyRepository vocabularyRepository
    ) {
        this.quizHistoryRepository = quizHistoryRepository;
        this.sessionStore = sessionStore;
        this.wrongAnswerRepository = wrongAnswerRepository;
        this.learningProgressRepository = learningProgressRepository;
        this.vocabularyRepository = vocabularyRepository;
    }

    @Transactional
    public QuizHistoryResponse completeQuiz(QuizCompletionRequest request) {
        if (request.wrongAnswerReview()) {
            throw new QuizHistoryCompletionException("Wrong answer review quizzes are not included in general learning statistics.");
        }

        Set<String> questionIds = new LinkedHashSet<>(request.questionIds());
        if (questionIds.size() != request.questionIds().size()) {
            throw new QuizHistoryCompletionException("Question IDs must be unique.");
        }

        List<QuizQuestionSubmissionResult> results = questionIds.stream()
                .map(questionId -> sessionStore.findSubmissionResult(questionId)
                        .orElseThrow(() -> new QuizHistoryCompletionException("All quiz questions must be submitted before completion.")))
                .toList();

        boolean hasDifferentCategory = results.stream()
                .anyMatch(result -> result.category() != request.category());
        if (hasDifferentCategory) {
            throw new QuizHistoryCompletionException("Submitted questions do not match the requested category.");
        }

        boolean hasDifferentMode = results.stream()
                .anyMatch(result -> request.mode() != QuizMode.MIXED && result.mode() != request.mode());
        if (hasDifferentMode) {
            throw new QuizHistoryCompletionException("Submitted questions do not match the requested quiz mode.");
        }

        int totalCount = results.size();
        int correctCount = (int) results.stream().filter(QuizQuestionSubmissionResult::correct).count();
        QuizHistory history = new QuizHistory(
                request.category(),
                request.mode(),
                totalCount,
                correctCount,
                totalCount - correctCount
        );

        return QuizHistoryResponse.from(quizHistoryRepository.save(history));
    }

    public StatisticsDashboardResponse dashboard() {
        List<QuizHistory> histories = quizHistoryRepository.findAllByOrderByCompletedAtDesc();
        LocalDate today = LocalDate.now();
        List<QuizHistory> todayHistories = histories.stream()
                .filter(history -> history.getCompletedAt().toLocalDate().isEqual(today))
                .toList();

        return new StatisticsDashboardResponse(
                summarize(histories),
                summarize(todayHistories),
                summarizeCategories(histories),
                summarizeModes(histories),
                quizHistoryRepository.findTop10ByOrderByCompletedAtDesc()
                        .stream()
                        .map(QuizHistoryResponse::from)
                        .toList(),
                wrongAnswerRepository.findTop5ByOrderByWrongCountDescLastWrongAtDesc()
                        .stream()
                        .map(MostWrongVocabularyResponse::from)
                        .toList(),
                learningProgressRepository.findTop20ByOrderByLastAttemptedAtDesc()
                        .stream()
                        .map(VocabularyLearningProgressResponse::from)
                        .toList(),
                vocabularyRepository.count(),
                learningProgressRepository.count(),
                vocabularyCounts()
        );
    }

    private List<VocabularyCountStat> vocabularyCounts() {
        return DASHBOARD_CATEGORIES.stream()
                .map(category -> new VocabularyCountStat(
                        category,
                        vocabularyRepository.countByCategory(category),
                        learningProgressRepository.countByVocabularyCategory(category)
                ))
                .toList();
    }

    private LearningCountSummary summarize(List<QuizHistory> histories) {
        int totalQuestionCount = histories.stream().mapToInt(QuizHistory::getTotalCount).sum();
        int correctCount = histories.stream().mapToInt(QuizHistory::getCorrectCount).sum();
        return LearningCountSummary.from(totalQuestionCount, correctCount, histories.size());
    }

    private List<CategoryLearningStat> summarizeCategories(List<QuizHistory> histories) {
        Map<VocabularyCategory, List<QuizHistory>> grouped = histories.stream()
                .collect(Collectors.groupingBy(QuizHistory::getCategory));

        List<CategoryLearningStat> stats = new ArrayList<>();
        for (VocabularyCategory category : DASHBOARD_CATEGORIES) {
            stats.add(categoryStat(category, grouped.getOrDefault(category, List.of())));
        }
        return stats;
    }

    private CategoryLearningStat categoryStat(VocabularyCategory category, List<QuizHistory> histories) {
        int totalQuestionCount = histories.stream().mapToInt(QuizHistory::getTotalCount).sum();
        int correctCount = histories.stream().mapToInt(QuizHistory::getCorrectCount).sum();
        return CategoryLearningStat.from(category, totalQuestionCount, correctCount);
    }

    private List<ModeLearningStat> summarizeModes(List<QuizHistory> histories) {
        Map<QuizMode, List<QuizHistory>> grouped = histories.stream()
                .collect(Collectors.groupingBy(QuizHistory::getQuizMode));

        return DASHBOARD_MODES.stream()
                .map(mode -> modeStat(mode, grouped.getOrDefault(mode, List.of())))
                .toList();
    }

    private ModeLearningStat modeStat(QuizMode mode, List<QuizHistory> histories) {
        int totalQuestionCount = histories.stream().mapToInt(QuizHistory::getTotalCount).sum();
        int correctCount = histories.stream().mapToInt(QuizHistory::getCorrectCount).sum();
        return ModeLearningStat.from(mode, totalQuestionCount, correctCount);
    }
}
