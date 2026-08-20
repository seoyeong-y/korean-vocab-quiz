CREATE TABLE vocabularies (
    id BIGINT NOT NULL AUTO_INCREMENT,
    word VARCHAR(100) NOT NULL,
    meaning VARCHAR(500) NOT NULL,
    example_sentence VARCHAR(1000),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
);
