package com.courseregistration.model;

public record QuizAttemptAnswer(
        Long id,
        Long attemptId,
        Long questionId,
        Long selectedOptionId,
        boolean wasCorrect
) {}
