-- =====================================================================
-- Seed data. Run this after schema.sql.
--
-- ADMIN and REGISTRAR accounts are NOT self-registrable through the
-- API on purpose — in a real system you don't want anyone signing up
-- as the person who approves their own enrollment. They're created
-- here directly. Change these passwords before you show this to anyone.
--
-- Login          | Password       | Role
-- ---------------|----------------|-----------
-- admin           | admin123       | ADMIN
-- registrar       | registrar123   | REGISTRAR
--
-- (Password hashes below are real BCrypt hashes of the passwords above,
-- generated with cost factor 10 — compatible with Spring Security's
-- BCryptPasswordEncoder out of the box.)
-- =====================================================================

USE course_registration;

INSERT INTO users (username, password_hash, full_name, email, role) VALUES
('admin',     '$2b$10$5pEaUqNWtblwQAdjfWtE3uD25xTAHDMj/.3WsPWmVtvhqImdk/WAG', 'Catalog Admin',     'admin@campus.edu',     'ADMIN'),
('registrar', '$2b$10$tGufdRV0MIyFcDWybU3Icu03xwTRLeU210vpbURXLcvd2Ngbc/Ssa', 'Office Registrar',  'registrar@campus.edu', 'REGISTRAR');

-- A handful of sample courses so the catalog isn't empty on first run.
INSERT INTO courses (code, title, description, credits, instructor_name, day_of_week, start_time, end_time, semester, capacity) VALUES
('CS101', 'Introduction to Programming', 'Fundamentals of programming using Java: variables, control flow, OOP basics.', 4, 'Dr. A. Sharma',   'MONDAY',    '09:00:00', '10:30:00', 'Fall 2026', 3),
('CS201', 'Data Structures & Algorithms', 'Arrays, lists, trees, graphs, sorting/searching, and complexity analysis.',    4, 'Dr. R. Verma',    'TUESDAY',   '11:00:00', '12:30:00', 'Fall 2026', 2),
('MA150', 'Discrete Mathematics',        'Logic, set theory, combinatorics, and graph theory for CS students.',        3, 'Prof. K. Iyer',   'WEDNESDAY', '13:00:00', '14:00:00', 'Fall 2026', 3),
('CS310', 'Database Systems',            'Relational model, SQL, normalization, transactions, and indexing.',         4, 'Dr. S. Rao',      'THURSDAY',  '10:00:00', '11:30:00', 'Fall 2026', 2),
('CS405', 'Web Application Development', 'Full-stack web development: REST APIs, auth, and modern JS frontends.',     3, 'Dr. A. Sharma',   'FRIDAY',    '09:00:00', '10:30:00', 'Fall 2026', 3);
