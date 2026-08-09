package com.courseregistration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ContentRequest(
        @NotBlank String title,
        @NotNull String contentType,   // "TEXT" or "LINK"
        String body,                   // required when contentType = TEXT
        String externalUrl,            // required when contentType = LINK
        int orderIndex
) {}
