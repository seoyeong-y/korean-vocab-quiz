package com.koreanvocabquiz.statistics;

import java.time.LocalDateTime;

import com.koreanvocabquiz.quiz.QuizMode;
import com.koreanvocabquiz.vocabulary.VocabularyCategory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "quiz_histories")
public class QuizHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private VocabularyCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "quiz_mode", nullable = false, length = 50)
    private QuizMode quizMode;

    @Column(name = "total_count", nullable = false)
    private int totalCount;

    @Column(name = "correct_count", nullable = false)
    private int correctCount;

    @Column(name = "incorrect_count", nullable = false)
    private int incorrectCount;

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected QuizHistory() {
    }

    public QuizHistory(VocabularyCategory category, QuizMode quizMode, int totalCount, int correctCount, int incorrectCount) {
        this.category = category;
        this.quizMode = quizMode;
        this.totalCount = totalCount;
        this.correctCount = correctCount;
        this.incorrectCount = incorrectCount;
        this.completedAt = LocalDateTime.now();
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (completedAt == null) {
            completedAt = now;
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public VocabularyCategory getCategory() {
        return category;
    }

    public QuizMode getQuizMode() {
        return quizMode;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int getCorrectCount() {
        return correctCount;
    }

    public int getIncorrectCount() {
        return incorrectCount;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
}
