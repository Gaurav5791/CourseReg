package com.courseregistration.model;

import java.time.LocalDateTime;

public record Enrollment(
        Long id,
        Long studentId,
        Long courseId,
        EnrollmentStatus status,
        LocalDateTime requestedAt,
        LocalDateTime decidedAt,
        Long decidedBy,
        String remarks
) {}
