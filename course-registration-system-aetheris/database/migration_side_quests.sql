-- =====================================================================
-- Migration: side quests (admin-authored bonus objectives per course,
-- self-reported complete by the student — lighter-weight than lessons
-- or quizzes, meant purely for engagement/points, not grading).
-- =====================================================================

USE course_registration;

CREATE TABLE side_quests (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id    BIGINT NOT NULL,
    title        VARCHAR(150) NOT NULL,
    description  TEXT,
    points       INT NOT NULL DEFAULT 10,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_quest_course FOREIGN KEY (course_id) REFERENCES courses(id)
) ENGINE=InnoDB;

CREATE TABLE side_quest_completions (
    student_id     BIGINT NOT NULL,
    quest_id       BIGINT NOT NULL,
    completed_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (student_id, quest_id),
    CONSTRAINT fk_qc_student FOREIGN KEY (student_id) REFERENCES users(id),
    CONSTRAINT fk_qc_quest   FOREIGN KEY (quest_id) REFERENCES side_quests(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_quest_course ON side_quests(course_id);
