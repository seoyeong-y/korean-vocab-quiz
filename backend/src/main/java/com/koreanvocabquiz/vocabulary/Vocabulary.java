package com.koreanvocabquiz.vocabulary;

import java.time.LocalDateTime;

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
@Table(name = "vocabularies")
public class Vocabulary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String word;

    @Column(nullable = false, length = 500)
    private String meaning;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private VocabularyCategory category;

    @Column(name = "example_sentence", length = 1000)
    private String exampleSentence;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Vocabulary() {
    }

    public Vocabulary(String word, String meaning, String exampleSentence) {
        this(word, meaning, VocabularyCategory.GENERAL, exampleSentence);
    }

    public Vocabulary(String word, String meaning, VocabularyCategory category, String exampleSentence) {
        this.word = word;
        this.meaning = meaning;
        this.category = category;
        this.exampleSentence = exampleSentence;
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

    public void update(String word, String meaning, VocabularyCategory category, String exampleSentence) {
        this.word = word;
        this.meaning = meaning;
        this.category = category;
        this.exampleSentence = exampleSentence;
    }

    public Long getId() {
        return id;
    }

    public String getWord() {
        return word;
    }

    public String getMeaning() {
        return meaning;
    }

    public VocabularyCategory getCategory() {
        return category;
    }

    public String getExampleSentence() {
        return exampleSentence;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
