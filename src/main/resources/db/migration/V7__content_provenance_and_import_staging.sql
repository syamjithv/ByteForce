-- ByteForce Schema Migration: V7__content_provenance_and_import_staging.sql
-- 1. Extend questions table with provenance and review status
ALTER TABLE questions ADD COLUMN source_repo VARCHAR(255) NULL;
ALTER TABLE questions ADD COLUMN source_url VARCHAR(500) NULL;
ALTER TABLE questions ADD COLUMN license VARCHAR(100) NULL;
ALTER TABLE questions ADD COLUMN author VARCHAR(255) NULL;
ALTER TABLE questions ADD COLUMN attribution VARCHAR(500) NULL;
ALTER TABLE questions ADD COLUMN review_status VARCHAR(50) NOT NULL DEFAULT 'PUBLISHED';
CREATE INDEX idx_questions_review_status ON questions(review_status);

-- 2. Extend aptitude_questions table with provenance and review status
ALTER TABLE aptitude_questions ADD COLUMN source_repo VARCHAR(255) NULL;
ALTER TABLE aptitude_questions ADD COLUMN source_url VARCHAR(500) NULL;
ALTER TABLE aptitude_questions ADD COLUMN license VARCHAR(100) NULL;
ALTER TABLE aptitude_questions ADD COLUMN author VARCHAR(255) NULL;
ALTER TABLE aptitude_questions ADD COLUMN attribution VARCHAR(500) NULL;
ALTER TABLE aptitude_questions ADD COLUMN review_status VARCHAR(50) NOT NULL DEFAULT 'PUBLISHED';
CREATE INDEX idx_aptitude_review_status ON aptitude_questions(review_status);

-- 3. Staging and Import Audit Log Table
CREATE TABLE IF NOT EXISTS staged_questions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id VARCHAR(64) NOT NULL,
    source_repo VARCHAR(255) NOT NULL,
    source_path VARCHAR(500) NULL,
    license VARCHAR(100) NOT NULL,
    author VARCHAR(255) NULL,
    attribution VARCHAR(500) NULL,
    category_or_subject VARCHAR(100) NOT NULL,
    topic VARCHAR(100) NOT NULL,
    difficulty VARCHAR(20) NOT NULL,
    question_type VARCHAR(50) NOT NULL,
    question_text TEXT NOT NULL,
    option_a TEXT NULL,
    option_b TEXT NULL,
    option_c TEXT NULL,
    option_d TEXT NULL,
    correct_answer VARCHAR(50) NULL,
    solution TEXT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'STAGED',
    rejection_reason VARCHAR(255) NULL,
    content_hash VARCHAR(64) NOT NULL,
    target_table VARCHAR(50) NULL,
    target_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_staged_batch ON staged_questions(batch_id);
CREATE INDEX idx_staged_status ON staged_questions(status);
CREATE INDEX idx_staged_hash ON staged_questions(content_hash);

-- 4. Ensure canonical placement curriculum subjects exist
INSERT INTO subjects (id, name, description, display_order)
VALUES ('data-structures', 'Data Structures & Algorithms', 'Fundamental and advanced data structures and algorithms.', 1)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO subjects (id, name, description, display_order)
VALUES ('dbms', 'Database Management Systems', 'Relational databases, SQL, ACID transactions, and indexing.', 2)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO subjects (id, name, description, display_order)
VALUES ('operating-systems', 'Operating Systems', 'Processes, threads, CPU scheduling, concurrency, memory management, and file systems.', 3)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO subjects (id, name, description, display_order)
VALUES ('computer-networks', 'Computer Networks', 'OSI model, TCP/IP, routing protocols, socket programming, and network security.', 4)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO subjects (id, name, description, display_order)
VALUES ('computer-organization', 'Computer Organization & Architecture', 'Digital logic, instruction set architecture, pipelining, and cache memory.', 5)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO subjects (id, name, description, display_order)
VALUES ('software-engineering', 'Software Engineering & System Design', 'System design, design patterns, microservices, and behavioral interview prep.', 6)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO subjects (id, name, description, display_order)
VALUES ('aptitude', 'Quantitative & Logical Aptitude', 'Quantitative aptitude, logical reasoning, verbal ability, and data interpretation.', 7)
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO subjects (id, name, description, display_order)
VALUES ('theory-of-computation', 'Theory of Computation', 'Automata theory, formal languages, grammars, and Turing machines.', 8)
ON DUPLICATE KEY UPDATE name = VALUES(name);
