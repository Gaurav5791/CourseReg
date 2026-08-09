package com.courseregistration.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record SideQuestRequest(
        @NotBlank String title,
        String description,
        @Min(1) int points
) {}
