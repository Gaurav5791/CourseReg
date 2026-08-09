package com.courseregistration.dto;

import jakarta.validation.constraints.NotNull;

public record AnswerSubmission(
        @NotNull Long questionId,
        Long selectedOptionId   // null is allowed — treated as "left blank"
) {}
