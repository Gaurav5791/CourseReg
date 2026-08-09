package com.courseregistration.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CourseRequest(
        @NotBlank String code,
        @NotBlank String title,
        String description,
        @Min(1) int credits,
        @NotBlank String instructorName,
        @NotBlank @Pattern(regexp = "MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY",
                message = "dayOfWeek must be an uppercase day name, e.g. MONDAY")
        String dayOfWeek,
        @NotBlank String startTime,   // "HH:mm" or "HH:mm:ss", parsed in the service layer
        @NotBlank String endTime,
        @NotBlank String semester,
        @NotNull @Min(1) Integer capacity
) {}
