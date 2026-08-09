package com.courseregistration.model;

public record QuizOption(
        Long id,
        Long questionId,
        String optionText,
        boolean correct,
        int orderIndex
) {}
