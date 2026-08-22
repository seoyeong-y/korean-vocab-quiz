package com.koreanvocabquiz.literature;

import java.time.LocalDateTime;

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

@Entity
@Table(name = "literary_features")
public class LiteraryFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private LiteraryAuthor author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_id")
    private LiteraryWork work;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LiteratureFeatureType type;

    @Column(nullable = false, length = 400)
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected LiteraryFeature() {
    }

    public LiteraryFeature(LiteraryAuthor author, LiteraryWork work, LiteratureFeatureType type, String content) {
        this.author = author;
        this.work = work;
        this.type = type;
        this.content = content;
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() { updatedAt = LocalDateTime.now(); }

    public void update(LiteraryAuthor author, LiteraryWork work, LiteratureFeatureType type, String content) {
        this.author = author;
        this.work = work;
        this.type = type;
        this.content = content;
    }

    public Long getId() { return id; }
    public LiteraryAuthor getAuthor() { return author; }
    public LiteraryWork getWork() { return work; }
    public LiteratureFeatureType getType() { return type; }
    public String getContent() { return content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
