CREATE TABLE quiz_histories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    category VARCHAR(50) NOT NULL,
    quiz_mode VARCHAR(50) NOT NULL,
    total_count INT NOT NULL,
    correct_count INT NOT NULL,
    incorrect_count INT NOT NULL,
    completed_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_quiz_histories_completed_at
    ON quiz_histories (completed_at);

CREATE INDEX idx_quiz_histories_category
    ON quiz_histories (category);

CREATE INDEX idx_quiz_histories_quiz_mode
    ON quiz_histories (quiz_mode);
