-- =====================================================================
-- Migration: career paths (admin curates a path; students see which
-- required courses they've completed, are currently in, or still need).
-- =====================================================================

USE course_registration;

CREATE TABLE career_paths (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(150) NOT NULL,
    description  TEXT,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE career_path_courses (
    career_path_id  BIGINT NOT NULL,
    course_id       BIGINT NOT NULL,
    order_index     INT NOT NULL DEFAULT 0,
    PRIMARY KEY (career_path_id, course_id),
    CONSTRAINT fk_pathcourse_path   FOREIGN KEY (career_path_id) REFERENCES career_paths(id) ON DELETE CASCADE,
    CONSTRAINT fk_pathcourse_course FOREIGN KEY (course_id) REFERENCES courses(id)
) ENGINE=InnoDB;
