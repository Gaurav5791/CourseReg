-- =====================================================================
-- Migration: course content (lessons)
-- Run this AFTER schema.sql / seed_data.sql — it only adds a new table,
-- it doesn't touch anything you already have.
-- =====================================================================

USE course_registration;

CREATE TABLE course_contents (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id     BIGINT NOT NULL,
    title         VARCHAR(150) NOT NULL,
    content_type  ENUM('TEXT', 'LINK') NOT NULL DEFAULT 'TEXT',
    body          TEXT,           -- used when content_type = TEXT (lesson notes, instructions, etc.)
    external_url  VARCHAR(500),   -- used when content_type = LINK (a video link, an article, etc.)
    order_index   INT NOT NULL DEFAULT 0,   -- controls display order within the course
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_content_course FOREIGN KEY (course_id) REFERENCES courses(id)
) ENGINE=InnoDB;

CREATE INDEX idx_content_course ON course_contents(course_id);
