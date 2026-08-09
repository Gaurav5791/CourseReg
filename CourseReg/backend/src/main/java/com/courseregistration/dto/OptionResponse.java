package com.courseregistration.dto;

/** correct is null when a student is taking the quiz (don't leak the answer), and set for admin/results views. */
public record OptionResponse(
        Long id,
        String optionText,
        Boolean correct
) {}
