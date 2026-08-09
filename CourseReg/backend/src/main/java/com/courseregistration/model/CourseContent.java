package com.courseregistration.model;

import java.time.LocalDateTime;

public record CourseContent(
        Long id,
        Long courseId,
        String title,
        ContentType contentType,
        String body,
        String externalUrl,
        int orderIndex,
        LocalDateTime createdAt
) {}
