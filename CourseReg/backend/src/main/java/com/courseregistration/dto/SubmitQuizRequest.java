package com.courseregistration.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SubmitQuizRequest(
        @NotEmpty @Valid List<AnswerSubmission> answers
) {}
