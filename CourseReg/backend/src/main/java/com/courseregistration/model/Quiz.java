package com.courseregistration.model;

import java.time.LocalDateTime;

public record Quiz(
        Long id,
        Long courseId,
        String title,
        String description,
        LocalDateTime createdAt
) {}
