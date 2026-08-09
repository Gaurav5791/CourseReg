package com.courseregistration.controller;

import com.courseregistration.dto.*;
import com.courseregistration.security.AuthenticatedUser;
import com.courseregistration.service.CareerPathService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CareerPathController {

    private final CareerPathService careerPathService;

    public CareerPathController(CareerPathService careerPathService) {
        this.careerPathService = careerPathService;
    }

    // ---------- Browsing (any authenticated role) ----------

    @GetMapping("/api/career-paths")
    public List<CareerPathResponse> listAll() {
        return careerPathService.listAll();
    }

    // ---------- Admin ----------

    @PostMapping("/api/admin/career-paths")
    public CareerPathResponse create(@Valid @RequestBody CareerPathRequest req) {
        return careerPathService.create(req);
    }

    @GetMapping("/api/admin/career-paths/{pathId}")
    public CareerPathAdminDetail getAdminDetail(@PathVariable Long pathId) {
        return careerPathService.getAdminDetail(pathId);
    }

    @PostMapping("/api/admin/career-paths/{pathId}/courses")
    public ResponseEntity<Void> addCourse(@PathVariable Long pathId, @Valid @RequestBody CareerPathCourseRequest req) {
        careerPathService.addCourse(pathId, req);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/admin/career-paths/{pathId}/courses/{courseId}")
    public ResponseEntity<Void> removeCourse(@PathVariable Long pathId, @PathVariable Long courseId) {
        careerPathService.removeCourse(pathId, courseId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/admin/career-paths/{pathId}")
    public ResponseEntity<Void> deletePath(@PathVariable Long pathId) {
        careerPathService.deletePath(pathId);
        return ResponseEntity.noContent().build();
    }

    // ---------- Student ----------

    @GetMapping("/api/student/career-paths/{pathId}")
    public CareerPathStudentDetail getStudentDetail(@AuthenticationPrincipal AuthenticatedUser user,
                                                     @PathVariable Long pathId) {
        return careerPathService.getStudentDetail(user.userId(), pathId);
    }
}
