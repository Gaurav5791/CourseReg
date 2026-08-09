package com.courseregistration.service;

import com.courseregistration.dao.CareerPathDao;
import com.courseregistration.dao.CertificateDao;
import com.courseregistration.dao.CourseDao;
import com.courseregistration.dao.EnrollmentDao;
import com.courseregistration.dto.*;
import com.courseregistration.exception.ApiException;
import com.courseregistration.model.CareerPath;
import com.courseregistration.model.Course;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CareerPathService {

    private final CareerPathDao pathDao;
    private final CourseDao courseDao;
    private final EnrollmentDao enrollmentDao;
    private final CertificateDao certificateDao;

    public CareerPathService(CareerPathDao pathDao, CourseDao courseDao, EnrollmentDao enrollmentDao,
                              CertificateDao certificateDao) {
        this.pathDao = pathDao;
        this.courseDao = courseDao;
        this.enrollmentDao = enrollmentDao;
        this.certificateDao = certificateDao;
    }

    // ---------- Shared / browsing (both admin and student use this to list paths) ----------

    public List<CareerPathResponse> listAll() {
        return pathDao.findAll().stream()
                .map(p -> new CareerPathResponse(p.id(), p.name(), p.description(), pathDao.countCoursesForPath(p.id())))
                .toList();
    }

    // ---------- Admin ----------

    public CareerPathResponse create(CareerPathRequest req) {
        CareerPath path = pathDao.insert(req.name(), req.description());
        return new CareerPathResponse(path.id(), path.name(), path.description(), 0);
    }

    public CareerPathAdminDetail getAdminDetail(Long pathId) {
        CareerPath path = pathDao.findById(pathId).orElseThrow(() -> new ApiException(404, "Career path not found"));
        List<CourseResponse> courses = pathDao.findCoursesForPath(pathId).stream()
                .map(CourseResponse::from).toList();
        return new CareerPathAdminDetail(path.id(), path.name(), path.description(), courses);
    }

    public void addCourse(Long pathId, CareerPathCourseRequest req) {
        pathDao.findById(pathId).orElseThrow(() -> new ApiException(404, "Career path not found"));
        courseDao.findById(req.courseId()).orElseThrow(() -> new ApiException(404, "Course not found"));
        pathDao.addCourse(pathId, req.courseId(), req.orderIndex());
    }

    public void removeCourse(Long pathId, Long courseId) {
        pathDao.removeCourse(pathId, courseId);
    }

    public void deletePath(Long pathId) {
        pathDao.delete(pathId);
    }

    // ---------- Student ----------

    public CareerPathStudentDetail getStudentDetail(Long studentId, Long pathId) {
        CareerPath path = pathDao.findById(pathId).orElseThrow(() -> new ApiException(404, "Career path not found"));

        List<Course> pathCourses = pathDao.findCoursesForPath(pathId);
        List<PathCourseStatus> courses = new ArrayList<>();
        for (int i = 0; i < pathCourses.size(); i++) {
            Course c = pathCourses.get(i);
            courses.add(new PathCourseStatus(c.id(), c.code(), c.title(), c.credits(), i, statusFor(studentId, c)));
        }

        return new CareerPathStudentDetail(path.id(), path.name(), path.description(), courses);
    }

    private String statusFor(Long studentId, Course course) {
        if (certificateDao.findByStudentAndCourse(studentId, course.id()).isPresent()) {
            return "COMPLETED";
        }
        if (enrollmentDao.findActiveByStudentAndCourse(studentId, course.id()).isPresent()) {
            return "ENROLLED";
        }
        return "AVAILABLE";
    }
}
