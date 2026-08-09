package com.courseregistration.dto;

public record QuizResponse(
        Long id,
        Long courseId,
        String title,
        String description,
        int questionCount
) {}
