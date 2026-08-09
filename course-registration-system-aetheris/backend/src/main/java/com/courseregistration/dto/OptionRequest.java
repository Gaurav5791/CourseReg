package com.courseregistration.dto;

import jakarta.validation.constraints.NotBlank;

public record OptionRequest(
        @NotBlank String optionText,
        boolean correct
) {}
