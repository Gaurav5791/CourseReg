package com.courseregistration.dto;

public record AuthResponse(
        String token,
        Long userId,
        String username,
        String fullName,
        String role
) {}
