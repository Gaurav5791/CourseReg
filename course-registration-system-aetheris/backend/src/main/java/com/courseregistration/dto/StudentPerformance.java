package com.courseregistration.dto;

public record StudentPerformance(
        Long studentId,
        String studentName,
        CourseProgressResponse progress
) {}
