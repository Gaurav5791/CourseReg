package com.courseregistration.dto;

public record PathCourseStatus(
        Long courseId,
        String code,
        String title,
        int credits,
        int orderIndex,
        String status   // "COMPLETED", "ENROLLED", or "AVAILABLE"
) {}
