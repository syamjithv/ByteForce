-- ByteForce Schema Migration: V10__user_concept_progress.sql
-- Supports:
-- 1. Concept explicit completion state ("Mark as learned" / "Unmark")
-- 2. Accurate learning history and last viewed tracking
-- 3. "Continue Learning" dashboard recommendation (most recently accessed unfinished concept)
-- 4. Topic and Subject aggregate concept progress metrics

CREATE TABLE IF NOT EXISTS user_concept_progress (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    concept_id BIGINT NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at TIMESTAMP NULL,
    last_viewed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_concept UNIQUE (user_id, concept_id),
    CONSTRAINT fk_ucp_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ucp_concept FOREIGN KEY (concept_id) REFERENCES concepts(id) ON DELETE CASCADE
);

CREATE INDEX idx_ucp_user_viewed ON user_concept_progress(user_id, last_viewed_at DESC);
CREATE INDEX idx_ucp_user_completed ON user_concept_progress(user_id, completed);
