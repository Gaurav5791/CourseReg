FEATURE 01: COURSE PREREQUISITES

Objective:
Enable administrators to assign prerequisite courses and stop
students from requesting courses they are not ready for.

DATABASE CHANGES:
- Create table: course_prerequisites
  - id (PK)
  - course_id (FK to courses.id)
  - prerequisite_course_id (FK to courses.id)
  - created_at
- Enforce foreign key constraints
- Reject self-prerequisites and cyclical prerequisite relationships

BACKEND:
- Add model
- Add DTO
- Add DAO
- Add service logic
- Add controller endpoints
- Add validation
  - verify course exists
  - verify prerequisite exists
  - disallow self-prerequisite entries
  - detect and prevent prerequisite cycles
- Add authorization checks

API:
POST   /api/admin/courses/{id}/prerequisites
GET    /api/admin/courses/{id}/prerequisites
DELETE /api/admin/courses/{id}/prerequisites/{prerequisiteId}

STUDENT BEHAVIOR:
When a student submits an enrollment request:
1. Verify required prerequisites.
2. Confirm completion of each prerequisite.
3. If any are incomplete, reject the request.
4. Return the list of missing prerequisites.

EXAMPLE RESPONSE:
{
  "eligible": false,
  "missingPrerequisites": [
    "Java Fundamentals",
    "Object-Oriented Programming"
  ]
}

FRONTEND:
- Display prerequisites on course details.
- Show whether the student is eligible.
- Disable enrollment when prerequisites are missing.
- Explain why the course cannot be requested.

SECURITY:
- Only ADMIN may add or remove prerequisites.
- Eligibility checks must happen on the server.
- Do not depend solely on frontend validation.

TESTING:
- Student with required prerequisites can enroll.
- Student without prerequisites is blocked.
- Admin can manage prerequisites.
- Student cannot modify prerequisite rules.