package com.courseregistration.dto;

public record StudentQuizSummary(
        Long id,
        Long courseId,
        String title,
        String description,
        int questionCount,
        boolean attempted,
        Integer bestScore   // null if never attempted
) {}
