package com.courseregistration.controller;

import com.courseregistration.dto.CourseRequest;
import com.courseregistration.dto.CourseResponse;
import com.courseregistration.service.CourseService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin owns the catalog: create and edit courses. Deliberately NO
 * delete endpoint here — removing a course is the registrar's call
 * (see RegistrarController), so the two roles have distinct powers.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final CourseService courseService;

    public AdminController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping("/courses")
    public List<CourseResponse> listAll() {
        return courseService.listAll();
    }

    @PostMapping("/courses")
    public CourseResponse create(@Valid @RequestBody CourseRequest req) {
        return courseService.create(req);
    }

    @PutMapping("/courses/{id}")
    public CourseResponse update(@PathVariable Long id, @Valid @RequestBody CourseRequest req) {
        return courseService.update(id, req);
    }
}
