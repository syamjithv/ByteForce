-- ByteForce Schema Migration: V2__core_foundation_enhancements.sql
-- 1. Question Type Support: multi-format placement questions (CODING, MCQ, SQL, CONCEPTUAL)
ALTER TABLE questions ADD COLUMN question_type VARCHAR(20) NOT NULL DEFAULT 'CODING';

-- 2. Assessment Foundation: difficulty rating and optional topic categorization
ALTER TABLE assessments ADD COLUMN difficulty VARCHAR(20) NOT NULL DEFAULT 'MEDIUM';
ALTER TABLE assessments ADD COLUMN topic_id BIGINT NULL;
ALTER TABLE assessments ADD CONSTRAINT fk_assessments_topic FOREIGN KEY (topic_id) REFERENCES topics(id) ON DELETE SET NULL;
CREATE INDEX idx_assessments_topic ON assessments(topic_id);

-- 3. Assessment Answers: question-level responses, scoring, and unanswered/status tracking
CREATE TABLE IF NOT EXISTS assessment_answers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    assessment_attempt_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    submitted_answer TEXT,
    marks_awarded INT NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'SKIPPED',
    answered_at TIMESTAMP NULL,
    CONSTRAINT uq_asst_answer UNIQUE (assessment_attempt_id, question_id),
    CONSTRAINT fk_asst_ans_attempt FOREIGN KEY (assessment_attempt_id) REFERENCES assessment_attempts(id) ON DELETE CASCADE,
    CONSTRAINT fk_asst_ans_question FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE
);

CREATE INDEX idx_asst_answers_attempt ON assessment_answers(assessment_attempt_id);
