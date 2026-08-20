CREATE TABLE wrong_answers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    vocabulary_id BIGINT NOT NULL,
    quiz_mode VARCHAR(50) NOT NULL,
    wrong_count INT NOT NULL,
    last_wrong_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_wrong_answers_vocabulary
        FOREIGN KEY (vocabulary_id)
        REFERENCES vocabularies (id)
        ON DELETE CASCADE,
    CONSTRAINT uk_wrong_answers_vocabulary_mode
        UNIQUE (vocabulary_id, quiz_mode)
);

CREATE INDEX idx_wrong_answers_last_wrong_at
    ON wrong_answers (last_wrong_at);
