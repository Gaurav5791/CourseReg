package com.courseregistration.dto;

import java.util.List;

public record CareerPathStudentDetail(
        Long id,
        String name,
        String description,
        List<PathCourseStatus> courses
) {}
