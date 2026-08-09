-- =====================================================================
-- Online Course Registration System — Schema
-- Roles: STUDENT, ADMIN (manages the course catalog), REGISTRAR
--        (approves/rejects enroll & drop requests, can remove courses)
-- =====================================================================

CREATE DATABASE IF NOT EXISTS course_registration
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE course_registration;

-- ---------------------------------------------------------------------
-- users: students, admins, and the registrar all live in one table,
-- distinguished by `role`. Only STUDENT can self-register via the API;
-- ADMIN/REGISTRAR rows are seeded directly (see seed_data.sql).
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(100) NOT NULL UNIQUE,
    role          ENUM('STUDENT', 'ADMIN', 'REGISTRAR') NOT NULL,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- courses: the catalog. capacity/seats_taken drive seat-limit
-- enforcement. status=REMOVED is a soft delete — only a REGISTRAR
-- can set it (see EnrollmentService / CourseService).
-- ---------------------------------------------------------------------
CREATE TABLE courses (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    code             VARCHAR(20)  NOT NULL UNIQUE,
    title            VARCHAR(150) NOT NULL,
    description      TEXT,
    credits          INT NOT NULL DEFAULT 3,
    instructor_name  VARCHAR(100) NOT NULL,
    day_of_week      VARCHAR(15)  NOT NULL,   -- e.g. MONDAY
    start_time       TIME NOT NULL,
    end_time         TIME NOT NULL,
    semester         VARCHAR(30)  NOT NULL,   -- e.g. 'Fall 2026'
    capacity         INT NOT NULL,
    seats_taken      INT NOT NULL DEFAULT 0,
    status           ENUM('ACTIVE', 'REMOVED') NOT NULL DEFAULT 'ACTIVE',
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_seats CHECK (seats_taken >= 0 AND seats_taken <= capacity)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- enrollments: the approval workflow lives here.
--   PENDING       -> student asked to enroll, awaiting registrar
--   APPROVED      -> registrar approved, seat is held
--   REJECTED      -> registrar denied the enroll request
--   DROP_PENDING  -> student (already APPROVED) asked to drop
--   DROPPED       -> registrar approved the drop, seat released
--   DROP_REJECTED -> registrar denied the drop, student stays enrolled
-- ---------------------------------------------------------------------
CREATE TABLE enrollments (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id    BIGINT NOT NULL,
    course_id     BIGINT NOT NULL,
    status        ENUM('PENDING','APPROVED','REJECTED','DROP_PENDING','DROPPED','DROP_REJECTED')
                  NOT NULL DEFAULT 'PENDING',
    requested_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    decided_at    TIMESTAMP NULL,
    decided_by    BIGINT NULL,
    remarks       VARCHAR(255),

    CONSTRAINT fk_enroll_student FOREIGN KEY (student_id) REFERENCES users(id),
    CONSTRAINT fk_enroll_course  FOREIGN KEY (course_id)  REFERENCES courses(id),
    CONSTRAINT fk_enroll_decider FOREIGN KEY (decided_by) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE INDEX idx_enrollments_student ON enrollments(student_id);
CREATE INDEX idx_enrollments_course  ON enrollments(course_id);
CREATE INDEX idx_enrollments_status  ON enrollments(status);
CREATE INDEX idx_courses_status      ON courses(status);
