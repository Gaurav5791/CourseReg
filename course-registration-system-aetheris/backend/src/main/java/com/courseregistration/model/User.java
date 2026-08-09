package com.courseregistration.model;

import java.time.LocalDateTime;

public record User(
        Long id,
        String username,
        String passwordHash,
        String fullName,
        String email,
        Role role,
        LocalDateTime createdAt
) {}
