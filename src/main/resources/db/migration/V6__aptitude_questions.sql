-- ByteForce Schema Migration: V6__aptitude_questions.sql
-- Aptitude Question Bank: Quantitative Aptitude, Logical Reasoning, Verbal Ability

CREATE TABLE IF NOT EXISTS aptitude_questions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category VARCHAR(50) NOT NULL,
    topic VARCHAR(100) NOT NULL,
    difficulty VARCHAR(20) NOT NULL,
    question TEXT NOT NULL,
    option_a TEXT NOT NULL,
    option_b TEXT NOT NULL,
    option_c TEXT NOT NULL,
    option_d TEXT NOT NULL,
    correct_answer VARCHAR(10) NOT NULL,
    explanation TEXT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_aptitude_category ON aptitude_questions(category);
CREATE INDEX idx_aptitude_topic ON aptitude_questions(topic);
CREATE INDEX idx_aptitude_difficulty ON aptitude_questions(difficulty);
CREATE INDEX idx_aptitude_active ON aptitude_questions(active);
