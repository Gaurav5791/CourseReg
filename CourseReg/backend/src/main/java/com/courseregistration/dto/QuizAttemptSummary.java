package com.courseregistration.dto;

public record QuizAttemptSummary(
        Long attemptId,
        int score,
        int totalQuestions,
        String submittedAt
) {}
