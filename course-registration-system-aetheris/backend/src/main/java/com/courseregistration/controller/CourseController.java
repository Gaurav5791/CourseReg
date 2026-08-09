package com.courseregistration.controller;

import com.courseregistration.dto.CourseResponse;
import com.courseregistration.service.CourseService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Browsing is open to any authenticated user (student, admin, or registrar). */
@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public List<CourseResponse> browse(@RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) String semester) {
        return courseService.browseActive(keyword, semester);
    }

    @GetMapping("/{id}")
    public CourseResponse getOne(@PathVariable Long id) {
        return courseService.getById(id);
    }
}
