package com.courseregistration.dto;

public record SideQuestResponse(
        Long id,
        Long courseId,
        String title,
        String description,
        int points,
        Boolean completed   // null for the admin view
) {}
