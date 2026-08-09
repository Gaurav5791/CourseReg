package com.courseregistration.controller;

import com.courseregistration.dto.Badge;
import com.courseregistration.dto.CourseProgressResponse;
import com.courseregistration.dto.StudentPerformance;
import com.courseregistration.security.AuthenticatedUser;
import com.courseregistration.service.AnalyticsService;
import com.courseregistration.service.BadgeService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final BadgeService badgeService;

    public AnalyticsController(AnalyticsService analyticsService, BadgeService badgeService) {
        this.analyticsService = analyticsService;
        this.badgeService = badgeService;
    }

    /** Admin: every approved student in a course, with their lesson/quiz completion. */
    @GetMapping("/api/admin/courses/{courseId}/analytics")
    public List<StudentPerformance> getCourseAnalytics(@PathVariable Long courseId) {
        return analyticsService.getCourseAnalytics(courseId);
    }

    /** Student: their own progress in a course, including whether they qualify for a certificate yet. */
    @GetMapping("/api/student/courses/{courseId}/progress")
    public CourseProgressResponse getMyProgress(@AuthenticationPrincipal AuthenticatedUser user,
                                                 @PathVariable Long courseId) {
        return analyticsService.getMyProgress(user.userId(), courseId);
    }

    /** Student: achievement badges earned across all their courses. */
    @GetMapping("/api/student/badges")
    public List<Badge> getMyBadges(@AuthenticationPrincipal AuthenticatedUser user) {
        return badgeService.getMyBadges(user.userId());
    }
}
