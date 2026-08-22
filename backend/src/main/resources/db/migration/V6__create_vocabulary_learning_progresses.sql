CREATE TABLE vocabulary_learning_progresses (
    id BIGINT NOT NULL AUTO_INCREMENT,
    vocabulary_id BIGINT NOT NULL,
    attempt_count INT NOT NULL,
    correct_count INT NOT NULL,
    incorrect_count INT NOT NULL,
    last_attempted_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_vocabulary_learning_progresses_vocabulary
        FOREIGN KEY (vocabulary_id)
        REFERENCES vocabularies (id)
        ON DELETE CASCADE,
    CONSTRAINT uk_vocabulary_learning_progresses_vocabulary
        UNIQUE (vocabulary_id)
);

CREATE INDEX idx_vocabulary_learning_progresses_attempt_count
    ON vocabulary_learning_progresses (attempt_count);

CREATE INDEX idx_vocabulary_learning_progresses_last_attempted_at
    ON vocabulary_learning_progresses (last_attempted_at);
