package com.courseregistration.service;

import com.courseregistration.dao.CourseDao;
import com.courseregistration.dao.EnrollmentDao;
import com.courseregistration.dao.UserDao;
import com.courseregistration.dto.StudentPerformance;
import com.courseregistration.dto.CourseProgressResponse;
import com.courseregistration.exception.ApiException;
import com.courseregistration.model.Enrollment;
import com.courseregistration.model.User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnalyticsService {

    private final CourseDao courseDao;
    private final EnrollmentDao enrollmentDao;
    private final UserDao userDao;
    private final ProgressService progressService;

    public AnalyticsService(CourseDao courseDao, EnrollmentDao enrollmentDao, UserDao userDao, ProgressService progressService) {
        this.courseDao = courseDao;
        this.enrollmentDao = enrollmentDao;
        this.userDao = userDao;
        this.progressService = progressService;
    }

    public List<StudentPerformance> getCourseAnalytics(Long courseId) {
        courseDao.findById(courseId).orElseThrow(() -> new ApiException(404, "Course not found"));

        List<Enrollment> approved = enrollmentDao.findApprovedByCourse(courseId);
        return approved.stream().map(e -> {
            User student = userDao.findById(e.studentId()).orElse(null);
            var progress = progressService.computeProgress(e.studentId(), courseId);
            return new StudentPerformance(e.studentId(), student != null ? student.fullName() : "Unknown", progress);
        }).toList();
    }

    public CourseProgressResponse getMyProgress(Long studentId, Long courseId) {
        courseDao.findById(courseId).orElseThrow(() -> new ApiException(404, "Course not found"));
        if (!enrollmentDao.isApprovedForCourse(studentId, courseId)) {
            throw new ApiException(403, "You need an approved enrollment in this course");
        }
        return progressService.computeProgress(studentId, courseId);
    }
}
