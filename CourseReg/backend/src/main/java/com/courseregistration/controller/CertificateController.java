package com.courseregistration.controller;

import com.courseregistration.dto.CertificateResponse;
import com.courseregistration.security.AuthenticatedUser;
import com.courseregistration.service.CertificateService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class CertificateController {

    private final CertificateService certificateService;

    public CertificateController(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @PostMapping("/api/student/courses/{courseId}/certificate/claim")
    public CertificateResponse claim(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long courseId) {
        return certificateService.claim(user.userId(), courseId);
    }

    @GetMapping("/api/student/courses/{courseId}/certificate")
    public CertificateResponse getMine(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long courseId) {
        return certificateService.getMine(user.userId(), courseId);
    }

    /**
     * Public, no login required — this is the whole point of a certificate:
     * anyone (a recruiter, e.g.) can paste in the code and confirm it's real.
     * Requires a matching rule in SecurityConfig (see permitAll for this path).
     */
    @GetMapping("/api/certificates/verify/{code}")
    public CertificateResponse verify(@PathVariable String code) {
        return certificateService.verify(code);
    }
}
