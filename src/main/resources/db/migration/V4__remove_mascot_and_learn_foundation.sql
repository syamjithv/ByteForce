-- ByteForce Schema Migration: V4__remove_mascot_and_learn_foundation.sql
-- 1. Remove Mascot system from student_profiles (keeping avatar_url for profile photos)
ALTER TABLE student_profiles DROP COLUMN mascot;

-- 2. Subjects table (Canonical curriculum domains)
CREATE TABLE IF NOT EXISTS subjects (
    id VARCHAR(100) PRIMARY KEY,
    name VARCHAR(150) NOT NULL UNIQUE,
    description TEXT,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. Topics enhancement: relate topics to subjects
ALTER TABLE topics ADD COLUMN subject_id VARCHAR(100) NULL AFTER id;
ALTER TABLE topics ADD CONSTRAINT fk_topics_subject FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE SET NULL;
CREATE INDEX idx_topics_subject ON topics(subject_id);

-- 4. Concepts table (Core placement learning units)
CREATE TABLE IF NOT EXISTS concepts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    topic_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(220) NOT NULL,
    short_explanation TEXT NOT NULL,
    key_points TEXT NULL,
    example TEXT NULL,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_concepts_topic FOREIGN KEY (topic_id) REFERENCES topics(id) ON DELETE CASCADE
);

CREATE INDEX idx_concepts_topic ON concepts(topic_id);
CREATE INDEX idx_concepts_slug ON concepts(slug);

-- 5. Learning Resources table (External references, cheatsheets, articles, videos)
CREATE TABLE IF NOT EXISTS learning_resources (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    concept_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    resource_type VARCHAR(30) NOT NULL,
    url VARCHAR(500) NOT NULL,
    description TEXT NULL,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_resources_concept FOREIGN KEY (concept_id) REFERENCES concepts(id) ON DELETE CASCADE
);

CREATE INDEX idx_resources_concept ON learning_resources(concept_id);
