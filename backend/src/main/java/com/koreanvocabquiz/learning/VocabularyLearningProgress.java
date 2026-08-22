package com.koreanvocabquiz.learning;

import java.time.LocalDateTime;

import com.koreanvocabquiz.vocabulary.Vocabulary;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "vocabulary_learning_progresses")
public class VocabularyLearningProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vocabulary_id", nullable = false, unique = true)
    private Vocabulary vocabulary;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "correct_count", nullable = false)
    private int correctCount;

    @Column(name = "incorrect_count", nullable = false)
    private int incorrectCount;

    @Column(name = "last_attempted_at", nullable = false)
    private LocalDateTime lastAttemptedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected VocabularyLearningProgress() {
    }

    public VocabularyLearningProgress(Vocabulary vocabulary) {
        this.vocabulary = vocabulary;
        this.lastAttemptedAt = LocalDateTime.now();
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (lastAttemptedAt == null) {
            lastAttemptedAt = now;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void recordAttempt(boolean correct) {
        attemptCount += 1;
        if (correct) {
            correctCount += 1;
        } else {
            incorrectCount += 1;
        }
        lastAttemptedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Vocabulary getVocabulary() {
        return vocabulary;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public int getCorrectCount() {
        return correctCount;
    }

    public int getIncorrectCount() {
        return incorrectCount;
    }

    public LocalDateTime getLastAttemptedAt() {
        return lastAttemptedAt;
    }
}
