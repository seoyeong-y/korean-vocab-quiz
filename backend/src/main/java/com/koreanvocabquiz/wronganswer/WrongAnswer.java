package com.koreanvocabquiz.wronganswer;

import java.time.LocalDateTime;

import com.koreanvocabquiz.quiz.QuizMode;
import com.koreanvocabquiz.vocabulary.Vocabulary;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "wrong_answers",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_wrong_answers_vocabulary_mode",
                columnNames = {"vocabulary_id", "quiz_mode"}
        )
)
public class WrongAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vocabulary_id", nullable = false)
    private Vocabulary vocabulary;

    @Enumerated(EnumType.STRING)
    @Column(name = "quiz_mode", nullable = false, length = 50)
    private QuizMode quizMode;

    @Column(name = "wrong_count", nullable = false)
    private int wrongCount;

    @Column(name = "last_wrong_at", nullable = false)
    private LocalDateTime lastWrongAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected WrongAnswer() {
    }

    public WrongAnswer(Vocabulary vocabulary, QuizMode quizMode) {
        this.vocabulary = vocabulary;
        this.quizMode = quizMode;
        this.wrongCount = 1;
        this.lastWrongAt = LocalDateTime.now();
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void increaseWrongCount() {
        wrongCount += 1;
        lastWrongAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Vocabulary getVocabulary() {
        return vocabulary;
    }

    public QuizMode getQuizMode() {
        return quizMode;
    }

    public int getWrongCount() {
        return wrongCount;
    }

    public LocalDateTime getLastWrongAt() {
        return lastWrongAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
