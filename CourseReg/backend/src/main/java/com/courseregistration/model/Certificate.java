package com.courseregistration.model;

import java.time.LocalDateTime;

public record Certificate(
        Long id,
        Long studentId,
        Long courseId,
        String certificateCode,
        LocalDateTime issuedAt
) {}
