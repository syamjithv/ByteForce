-- ByteForce Schema Migration: V8__memory_review_state.sql
-- Spaced Repetition Memory Review State (User + RememberItem persistent scheduling)

CREATE TABLE IF NOT EXISTS user_remember_reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    remember_item_id BIGINT NOT NULL,
    due_at TIMESTAMP NOT NULL,
    last_reviewed_at TIMESTAMP NULL,
    review_count INT NOT NULL DEFAULT 0,
    successful_review_count INT NOT NULL DEFAULT 0,
    state VARCHAR(32) NOT NULL DEFAULT 'NEW',
    difficulty DOUBLE NOT NULL DEFAULT 0.0,
    stability DOUBLE NOT NULL DEFAULT 0.0,
    retrievability DOUBLE NOT NULL DEFAULT 0.0,
    last_rating VARCHAR(32) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_urr_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_urr_remember_item FOREIGN KEY (remember_item_id) REFERENCES remember_items(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_remember_item UNIQUE (user_id, remember_item_id)
);

CREATE INDEX idx_urr_user_due ON user_remember_reviews(user_id, due_at);
CREATE INDEX idx_urr_user_item ON user_remember_reviews(user_id, remember_item_id);
CREATE INDEX idx_urr_item ON user_remember_reviews(remember_item_id);
