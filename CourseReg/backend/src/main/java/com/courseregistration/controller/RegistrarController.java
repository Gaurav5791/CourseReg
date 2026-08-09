package com.courseregistration.controller;

import com.courseregistration.dto.CourseResponse;
import com.courseregistration.dto.DecisionRequest;
import com.courseregistration.dto.EnrollmentResponse;
import com.courseregistration.security.AuthenticatedUser;
import com.courseregistration.service.CourseService;
import com.courseregistration.service.EnrollmentService;
import com.courseregistration.service.RegistrarService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * The "controls the whole system" role: approves/rejects every
 * enroll and drop request, and is the only role that can remove a
 * course outright.
 */
@RestController
@RequestMapping("/api/registrar")
public class RegistrarController {

    private final EnrollmentService enrollmentService;
    private final RegistrarService registrarService;
    private final CourseService courseService;

    public RegistrarController(EnrollmentService enrollmentService, RegistrarService registrarService, CourseService courseService) {
        this.enrollmentService = enrollmentService;
        this.registrarService = registrarService;
        this.courseService = courseService;
    }

    @GetMapping("/courses")
    public List<CourseResponse> listAllCourses() {
        return courseService.listAll();
    }

    @GetMapping("/enrollments/pending")
    public List<EnrollmentResponse> pendingEnrollments() {
        return enrollmentService.pendingEnrollments();
    }

    @GetMapping("/drops/pending")
    public List<EnrollmentResponse> pendingDrops() {
        return enrollmentService.pendingDrops();
    }

    @PostMapping("/enrollments/{id}/approve")
    public EnrollmentResponse approveEnrollment(@AuthenticationPrincipal AuthenticatedUser user,
                                                 @PathVariable Long id,
                                                 @RequestBody(required = false) DecisionRequest body) {
        String remarks = body != null ? body.remarks() : null;
        return enrollmentService.approveEnrollment(id, user.userId(), remarks);
    }

    @PostMapping("/enrollments/{id}/reject")
    public EnrollmentResponse rejectEnrollment(@AuthenticationPrincipal AuthenticatedUser user,
                                                @PathVariable Long id,
                                                @RequestBody(required = false) DecisionRequest body) {
        String remarks = body != null ? body.remarks() : null;
        return enrollmentService.rejectEnrollment(id, user.userId(), remarks);
    }

    @PostMapping("/drops/{id}/approve")
    public EnrollmentResponse approveDrop(@AuthenticationPrincipal AuthenticatedUser user,
                                           @PathVariable Long id,
                                           @RequestBody(required = false) DecisionRequest body) {
        String remarks = body != null ? body.remarks() : null;
        return enrollmentService.approveDrop(id, user.userId(), remarks);
    }

    @PostMapping("/drops/{id}/reject")
    public EnrollmentResponse rejectDrop(@AuthenticationPrincipal AuthenticatedUser user,
                                          @PathVariable Long id,
                                          @RequestBody(required = false) DecisionRequest body) {
        String remarks = body != null ? body.remarks() : null;
        return enrollmentService.rejectDrop(id, user.userId(), remarks);
    }

    @DeleteMapping("/courses/{id}")
    public ResponseEntity<Void> removeCourse(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        registrarService.removeCourse(id, user.userId());
        return ResponseEntity.noContent().build();
    }
}
