package com.courseregistration.dto;

import jakarta.validation.constraints.NotBlank;

public record CareerPathRequest(
        @NotBlank String name,
        String description
) {}
