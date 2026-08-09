package com.courseregistration.model;

/**
 * STUDENT   - browses courses, requests enroll/drop
 * ADMIN     - manages the course catalog (create/edit courses)
 * REGISTRAR - approves/rejects enroll & drop requests, can remove a course
 */
public enum Role {
    STUDENT, ADMIN, REGISTRAR
}
