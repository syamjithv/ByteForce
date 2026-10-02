-- ByteForce Schema Migration: V5__remember_and_brain_maps.sql
-- Phase 2: Knowledge Layer (Remember Quick Revision + Brain Maps Concept Relationships)

-- 1. Remember Items (Quick revision items attached to Concepts)
CREATE TABLE IF NOT EXISTS remember_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    concept_id BIGINT NOT NULL,
    type VARCHAR(40) NOT NULL,
    content TEXT NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_remember_items_concept FOREIGN KEY (concept_id) REFERENCES concepts(id) ON DELETE CASCADE
);

CREATE INDEX idx_remember_items_concept ON remember_items(concept_id);
CREATE INDEX idx_remember_items_type ON remember_items(type);

-- 2. Concept Relationships (Interconnected Brain Map knowledge network)
CREATE TABLE IF NOT EXISTS concept_relationships (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_concept_id BIGINT NOT NULL,
    target_concept_id BIGINT NOT NULL,
    relationship_type VARCHAR(40) NOT NULL,
    description VARCHAR(255) NULL,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cr_source FOREIGN KEY (source_concept_id) REFERENCES concepts(id) ON DELETE CASCADE,
    CONSTRAINT fk_cr_target FOREIGN KEY (target_concept_id) REFERENCES concepts(id) ON DELETE CASCADE,
    CONSTRAINT uq_concept_relationship UNIQUE (source_concept_id, target_concept_id, relationship_type)
);

CREATE INDEX idx_cr_source ON concept_relationships(source_concept_id);
CREATE INDEX idx_cr_target ON concept_relationships(target_concept_id);
