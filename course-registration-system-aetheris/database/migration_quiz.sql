-- =====================================================================
-- Migration: quizzes with auto-grading
-- Run AFTER schema.sql / seed_data.sql / migration_content.sql.
-- Only adds new tables — nothing existing is touched.
-- =====================================================================

USE course_registration;

CREATE TABLE quizzes (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id    BIGINT NOT NULL,
    title        VARCHAR(150) NOT NULL,
    description  TEXT,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_quiz_course FOREIGN KEY (course_id) REFERENCES courses(id)
) ENGINE=InnoDB;

CREATE TABLE quiz_questions (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    quiz_id        BIGINT NOT NULL,
    question_text  TEXT NOT NULL,
    order_index    INT NOT NULL DEFAULT 0,
    -- ON DELETE CASCADE: deleting a quiz cleans up its questions automatically
    CONSTRAINT fk_question_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE quiz_options (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id    BIGINT NOT NULL,
    option_text    VARCHAR(500) NOT NULL,
    is_correct     BOOLEAN NOT NULL DEFAULT FALSE,
    order_index    INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_option_question FOREIGN KEY (question_id) REFERENCES quiz_questions(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE quiz_attempts (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    quiz_id           BIGINT NOT NULL,
    student_id        BIGINT NOT NULL,
    score             INT NOT NULL,
    total_questions   INT NOT NULL,
    submitted_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_attempt_quiz FOREIGN KEY (quiz_id) REFERENCES quizzes(id) ON DELETE CASCADE,
    CONSTRAINT fk_attempt_student FOREIGN KEY (student_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE quiz_attempt_answers (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    attempt_id           BIGINT NOT NULL,
    question_id          BIGINT NOT NULL,
    selected_option_id   BIGINT NULL,   -- NULL means the student left it unanswered
    was_correct          BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_answer_attempt  FOREIGN KEY (attempt_id) REFERENCES quiz_attempts(id) ON DELETE CASCADE,
    CONSTRAINT fk_answer_question FOREIGN KEY (question_id) REFERENCES quiz_questions(id),
    CONSTRAINT fk_answer_option   FOREIGN KEY (selected_option_id) REFERENCES quiz_options(id)
) ENGINE=InnoDB;

CREATE INDEX idx_quiz_course ON quizzes(course_id);
CREATE INDEX idx_question_quiz ON quiz_questions(quiz_id);
CREATE INDEX idx_option_question ON quiz_options(question_id);
CREATE INDEX idx_attempt_quiz_student ON quiz_attempts(quiz_id, student_id);
CREATE INDEX idx_answer_attempt ON quiz_attempt_answers(attempt_id);
