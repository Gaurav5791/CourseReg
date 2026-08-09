package com.courseregistration.dto;

public record CourseProgressResponse(
        int lessonsCompleted,
        int totalLessons,
        int quizzesAttempted,
        int quizzesPassed,
        int totalQuizzes,
        Double avgQuizPercent,     // null if no quizzes attempted yet
        boolean eligibleForCertificate
) {}
