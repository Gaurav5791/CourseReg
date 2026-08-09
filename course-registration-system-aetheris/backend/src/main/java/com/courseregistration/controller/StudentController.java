package com.courseregistration.controller;

import com.courseregistration.dto.CourseResponse;
import com.courseregistration.dto.EnrollRequest;
import com.courseregistration.dto.EnrollmentResponse;
import com.courseregistration.security.AuthenticatedUser;
import com.courseregistration.service.EnrollmentService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student")
public class StudentController {

    private final EnrollmentService enrollmentService;

    public StudentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @PostMapping("/enrollments")
    public EnrollmentResponse requestEnroll(@AuthenticationPrincipal AuthenticatedUser user,
                                             @Valid @RequestBody EnrollRequest req) {
        return enrollmentService.requestEnroll(user.userId(), req.courseId());
    }

    @PostMapping("/enrollments/{id}/drop")
    public EnrollmentResponse requestDrop(@AuthenticationPrincipal AuthenticatedUser user,
                                           @PathVariable Long id) {
        return enrollmentService.requestDrop(user.userId(), id);
    }

    @GetMapping("/enrollments")
    public List<EnrollmentResponse> myEnrollments(@AuthenticationPrincipal AuthenticatedUser user) {
        return enrollmentService.myEnrollments(user.userId());
    }

    @GetMapping("/schedule")
    public List<CourseResponse> mySchedule(@AuthenticationPrincipal AuthenticatedUser user) {
        return enrollmentService.mySchedule(user.userId());
    }
}
