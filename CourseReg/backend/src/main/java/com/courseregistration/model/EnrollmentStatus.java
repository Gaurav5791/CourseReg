package com.courseregistration.model;

/**
 * PENDING       - student asked to enroll, awaiting the registrar
 * APPROVED      - registrar approved it, a seat is held
 * REJECTED      - registrar denied the enroll request
 * DROP_PENDING  - student (already APPROVED) asked to drop
 * DROPPED       - registrar approved the drop, seat released
 * DROP_REJECTED - registrar denied the drop; student stays enrolled
 */
public enum EnrollmentStatus {
    PENDING, APPROVED, REJECTED, DROP_PENDING, DROPPED, DROP_REJECTED
}
