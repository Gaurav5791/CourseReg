package com.courseregistration.dto;

public record EnrollmentResponse(
        Long id,
        Long studentId,
        String studentName,
        Long courseId,
        String courseCode,
        String courseTitle,
        String status,
        String requestedAt,
        String decidedAt,
        String remarks
) {}
