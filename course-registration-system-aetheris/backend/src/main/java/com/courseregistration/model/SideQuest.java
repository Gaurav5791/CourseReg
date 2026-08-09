package com.courseregistration.model;

import java.time.LocalDateTime;

public record SideQuest(
        Long id,
        Long courseId,
        String title,
        String description,
        int points,
        LocalDateTime createdAt
) {}
