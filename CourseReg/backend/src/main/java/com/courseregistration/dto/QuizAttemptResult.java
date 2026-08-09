package com.courseregistration.dto;

import java.util.List;

public record QuizAttemptResult(
        Long attemptId,
        Long quizId,
        int score,
        int totalQuestions,
        String submittedAt,
        List<QuestionResult> results
) {}
