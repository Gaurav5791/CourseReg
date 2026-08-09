package com.courseregistration.dto;

import jakarta.validation.constraints.NotBlank;

public record QuizRequest(
        @NotBlank String title,
        String description
) {}
