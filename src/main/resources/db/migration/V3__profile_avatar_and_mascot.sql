-- ByteForce Schema Migration: V3__profile_avatar_and_mascot.sql
-- Adds support for deployment-safe persistent avatars and ByteForce mascot choices

ALTER TABLE student_profiles ADD COLUMN avatar_url MEDIUMTEXT NULL;
ALTER TABLE student_profiles ADD COLUMN mascot VARCHAR(50) NULL DEFAULT 'bot';
