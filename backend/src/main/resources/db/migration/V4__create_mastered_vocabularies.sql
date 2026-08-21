CREATE TABLE mastered_vocabularies (
    id BIGINT NOT NULL AUTO_INCREMENT,
    vocabulary_id BIGINT NOT NULL,
    mastered_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_mastered_vocabularies_vocabulary
        FOREIGN KEY (vocabulary_id)
        REFERENCES vocabularies (id)
        ON DELETE CASCADE,
    CONSTRAINT uk_mastered_vocabularies_vocabulary
        UNIQUE (vocabulary_id)
);

CREATE INDEX idx_mastered_vocabularies_mastered_at
    ON mastered_vocabularies (mastered_at);
