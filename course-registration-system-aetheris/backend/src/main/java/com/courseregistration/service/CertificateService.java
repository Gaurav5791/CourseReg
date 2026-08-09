package com.courseregistration.service;

import com.courseregistration.dao.CertificateDao;
import com.courseregistration.dao.CourseDao;
import com.courseregistration.dao.EnrollmentDao;
import com.courseregistration.dao.UserDao;
import com.courseregistration.dto.CertificateResponse;
import com.courseregistration.exception.ApiException;
import com.courseregistration.model.Certificate;
import com.courseregistration.model.Course;
import com.courseregistration.model.User;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class CertificateService {

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CertificateDao certificateDao;
    private final CourseDao courseDao;
    private final UserDao userDao;
    private final EnrollmentDao enrollmentDao;
    private final ProgressService progressService;

    public CertificateService(CertificateDao certificateDao, CourseDao courseDao, UserDao userDao,
                               EnrollmentDao enrollmentDao, ProgressService progressService) {
        this.certificateDao = certificateDao;
        this.courseDao = courseDao;
        this.userDao = userDao;
        this.enrollmentDao = enrollmentDao;
        this.progressService = progressService;
    }

    /** Validates eligibility server-side — the "Claim" button being visible isn't what grants the certificate. */
    public CertificateResponse claim(Long studentId, Long courseId) {
        if (!enrollmentDao.isApprovedForCourse(studentId, courseId)) {
            throw new ApiException(403, "You need an approved enrollment in this course");
        }

        var existing = certificateDao.findByStudentAndCourse(studentId, courseId);
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        var progress = progressService.computeProgress(studentId, courseId);
        if (!progress.eligibleForCertificate()) {
            throw new ApiException(409, "Not eligible yet — finish all lessons and pass all quizzes first");
        }

        String code = UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        Certificate saved = certificateDao.insert(studentId, courseId, code);
        return toResponse(saved);
    }

    public CertificateResponse getMine(Long studentId, Long courseId) {
        var cert = certificateDao.findByStudentAndCourse(studentId, courseId)
                .orElseThrow(() -> new ApiException(404, "No certificate issued for this course yet"));
        return toResponse(cert);
    }

    /** Public verification — no auth required, anyone with the code can check it's real. */
    public CertificateResponse verify(String code) {
        var cert = certificateDao.findByCode(code)
                .orElseThrow(() -> new ApiException(404, "No certificate found with that code"));
        return toResponse(cert);
    }

    private CertificateResponse toResponse(Certificate cert) {
        User student = userDao.findById(cert.studentId()).orElse(null);
        Course course = courseDao.findById(cert.courseId()).orElse(null);
        return new CertificateResponse(
                cert.id(),
                cert.certificateCode(),
                student != null ? student.fullName() : "Unknown",
                course != null ? course.code() : "Unknown",
                course != null ? course.title() : "Unknown",
                cert.issuedAt().format(TS_FMT)
        );
    }
}
