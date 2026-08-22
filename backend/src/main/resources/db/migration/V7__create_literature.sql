CREATE TABLE literary_authors (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_literary_author_name UNIQUE (name)
);

CREATE TABLE literary_works (
    id BIGINT NOT NULL AUTO_INCREMENT,
    author_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_literary_work_author_title UNIQUE (author_id, title),
    CONSTRAINT fk_literary_work_author FOREIGN KEY (author_id) REFERENCES literary_authors (id) ON DELETE CASCADE
);

CREATE TABLE literary_features (
    id BIGINT NOT NULL AUTO_INCREMENT,
    author_id BIGINT NOT NULL,
    work_id BIGINT,
    type VARCHAR(20) NOT NULL,
    content VARCHAR(400) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_literary_feature_work UNIQUE (author_id, work_id, content),
    CONSTRAINT fk_literary_feature_author FOREIGN KEY (author_id) REFERENCES literary_authors (id) ON DELETE CASCADE,
    CONSTRAINT fk_literary_feature_work FOREIGN KEY (work_id) REFERENCES literary_works (id) ON DELETE CASCADE
);

CREATE INDEX idx_literary_works_author ON literary_works (author_id);
CREATE INDEX idx_literary_features_author ON literary_features (author_id);
CREATE INDEX idx_literary_features_work ON literary_features (work_id);
