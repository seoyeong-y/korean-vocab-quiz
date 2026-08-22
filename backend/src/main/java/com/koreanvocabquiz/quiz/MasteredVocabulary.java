package com.koreanvocabquiz.quiz;

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
@Table(name = "mastered_vocabularies")
public class MasteredVocabulary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vocabulary_id", nullable = false, unique = true)
    private Vocabulary vocabulary;

    @Column(name = "mastered_at", nullable = false)
    private LocalDateTime masteredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected MasteredVocabulary() {
    }

    public MasteredVocabulary(Vocabulary vocabulary) {
        this.vocabulary = vocabulary;
        this.masteredAt = LocalDateTime.now();
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (masteredAt == null) {
            masteredAt = now;
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

    public Vocabulary getVocabulary() {
        return vocabulary;
    }

    public LocalDateTime getMasteredAt() {
        return masteredAt;
    }
}
