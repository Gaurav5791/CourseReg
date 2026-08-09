package com.courseregistration.model;

import java.time.LocalDateTime;

public record QuizAttempt(
        Long id,
        Long quizId,
        Long studentId,
        int score,
        int totalQuestions,
        LocalDateTime submittedAt
) {}
