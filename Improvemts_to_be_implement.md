FEATURE 01: COURSE PREREQUISITES

Objective:
Allow administrators to define prerequisite courses and prevent
students from requesting courses for which they are not eligible.

DATABASE CHANGES:
- New table: course_prerequisites
  - id (PK)
  - course_id (FK to courses.id)
  - prerequisite_course_id (FK to courses.id)
  - created_at
- Enforce foreign-key constraints
- Prevent self-prerequisites and circular prerequisite chains

BACKEND:
- Add model
- Add DTO
- Add DAO
- Add service logic
- Add controller endpoints
- Add validation
  - course exists
  - prerequisite exists
  - no self-prerequisite
  - no prerequisite cycles
- Add authorization

API:
POST   /api/admin/courses/{id}/prerequisites
GET    /api/admin/courses/{id}/prerequisites
DELETE /api/admin/courses/{id}/prerequisites/{prerequisiteId}

STUDENT BEHAVIOR:
When a student requests enrollment:
1. Check prerequisites.
2. Check whether each prerequisite is completed.
3. If incomplete, reject the request.
4. Return the missing prerequisites.

EXAMPLE RESPONSE:
{
  "eligible": false,
  "missingPrerequisites": [
    "Java Fundamentals",
    "Object-Oriented Programming"
  ]
}

FRONTEND:
- Show prerequisites on course details.
- Show eligibility status.
- Disable enrollment when prerequisites are missing.
- Explain why enrollment is unavailable.

SECURITY:
- Only ADMIN can create/remove prerequisites.
- Student eligibility must be checked server-side.
- Never rely only on frontend validation.

TESTING:
- Student with prerequisites → enrollment allowed.
- Student without prerequisites → enrollment blocked.
- Admin → can manage prerequisites.
- Student → cannot modify prerequisites.