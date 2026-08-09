package com.courseregistration.dto;

import java.util.List;

public record QuestionResponse(
        Long id,
        String questionText,
        int orderIndex,
        List<OptionResponse> options
) {}
