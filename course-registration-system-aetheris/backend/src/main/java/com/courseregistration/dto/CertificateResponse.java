package com.courseregistration.dto;

public record CertificateResponse(
        Long id,
        String certificateCode,
        String studentName,
        String courseCode,
        String courseTitle,
        String issuedAt
) {}
