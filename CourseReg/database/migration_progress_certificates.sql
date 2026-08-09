-- =====================================================================
-- Migration: content completion tracking + certificates
-- Run AFTER the previous migrations. Only adds new tables.
-- =====================================================================

USE course_registration;

-- One row per (student, lesson) they've marked complete.
CREATE TABLE content_progress (
    student_id    BIGINT NOT NULL,
    content_id    BIGINT NOT NULL,
    completed_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (student_id, content_id),
    CONSTRAINT fk_progress_student FOREIGN KEY (student_id) REFERENCES users(id),
    CONSTRAINT fk_progress_content FOREIGN KEY (content_id) REFERENCES course_contents(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- One row per (student, course) once they've claimed a certificate.
-- certificate_code is the public verification code.
CREATE TABLE certificates (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id         BIGINT NOT NULL,
    course_id          BIGINT NOT NULL,
    certificate_code   VARCHAR(40) NOT NULL UNIQUE,
    issued_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_cert_student_course UNIQUE (student_id, course_id),
    CONSTRAINT fk_cert_student FOREIGN KEY (student_id) REFERENCES users(id),
    CONSTRAINT fk_cert_course FOREIGN KEY (course_id) REFERENCES courses(id)
) ENGINE=InnoDB;

CREATE INDEX idx_progress_student_content ON content_progress(student_id, content_id);
CREATE INDEX idx_cert_code ON certificates(certificate_code);
