package com.courseregistration.service;

import com.courseregistration.dao.CourseDao;
import com.courseregistration.dao.EnrollmentDao;
import com.courseregistration.dao.UserDao;
import com.courseregistration.dto.CourseResponse;
import com.courseregistration.dto.EnrollmentResponse;
import com.courseregistration.exception.ApiException;
import com.courseregistration.model.Course;
import com.courseregistration.model.CourseStatus;
import com.courseregistration.model.Enrollment;
import com.courseregistration.model.EnrollmentStatus;
import com.courseregistration.model.User;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class EnrollmentService {

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final EnrollmentDao enrollmentDao;
    private final CourseDao courseDao;
    private final UserDao userDao;
    private final DataSource dataSource;

    public EnrollmentService(EnrollmentDao enrollmentDao, CourseDao courseDao, UserDao userDao, DataSource dataSource) {
        this.enrollmentDao = enrollmentDao;
        this.courseDao = courseDao;
        this.userDao = userDao;
        this.dataSource = dataSource;
    }

    // ------------------------------------------------------------------
    // Student actions
    // ------------------------------------------------------------------

    public EnrollmentResponse requestEnroll(Long studentId, Long courseId) {
        Course course = courseDao.findById(courseId)
                .orElseThrow(() -> new ApiException(404, "Course not found"));

        if (course.status() != CourseStatus.ACTIVE) {
            throw new ApiException(409, "This course is no longer offered");
        }
        if (course.seatsAvailable() <= 0) {
            throw new ApiException(409, "This course is full");
        }
        if (enrollmentDao.findActiveByStudentAndCourse(studentId, courseId).isPresent()) {
            throw new ApiException(409, "You already have an active request or enrollment for this course");
        }

        Enrollment enrollment = enrollmentDao.insertPending(studentId, courseId);
        return enrich(enrollment);
    }

    public EnrollmentResponse requestDrop(Long studentId, Long enrollmentId) {
        Enrollment enrollment = enrollmentDao.findById(enrollmentId)
                .orElseThrow(() -> new ApiException(404, "Enrollment not found"));

        if (!enrollment.studentId().equals(studentId)) {
            throw new ApiException(403, "That's not your enrollment");
        }
        if (enrollment.status() != EnrollmentStatus.APPROVED) {
            throw new ApiException(409, "Only an approved enrollment can be dropped");
        }

        enrollmentDao.updateStatusSimple(enrollmentId, EnrollmentStatus.DROP_PENDING);
        return enrich(enrollmentDao.findById(enrollmentId).orElseThrow());
    }

    public List<EnrollmentResponse> myEnrollments(Long studentId) {
        return enrollmentDao.findByStudent(studentId).stream().map(this::enrich).toList();
    }

    /** Only APPROVED enrollments count as "on the schedule". */
    public List<CourseResponse> mySchedule(Long studentId) {
        return enrollmentDao.findByStudent(studentId).stream()
                .filter(e -> e.status() == EnrollmentStatus.APPROVED)
                .map(e -> courseDao.findById(e.courseId()).orElseThrow())
                .map(CourseResponse::from)
                .toList();
    }

    // ------------------------------------------------------------------
    // Registrar actions
    // ------------------------------------------------------------------

    public List<EnrollmentResponse> pendingEnrollments() {
        return enrollmentDao.findByStatus(EnrollmentStatus.PENDING).stream().map(this::enrich).toList();
    }

    public List<EnrollmentResponse> pendingDrops() {
        return enrollmentDao.findByStatus(EnrollmentStatus.DROP_PENDING).stream().map(this::enrich).toList();
    }

    public EnrollmentResponse approveEnrollment(Long enrollmentId, Long registrarId, String remarks) {
        return runInTransaction(conn -> {
            Enrollment enrollment = lockEnrollment(conn, enrollmentId, EnrollmentStatus.PENDING);
            Course course = lockCourse(conn, enrollment.courseId());

            if (course.seatsAvailable() <= 0) {
                throw new ApiException(409, "Course filled up before this request could be approved");
            }

            courseDao.adjustSeatsTaken(conn, course.id(), 1);
            enrollmentDao.recordDecision(conn, enrollmentId, EnrollmentStatus.APPROVED, registrarId, remarks);
            return enrollmentId;
        });
    }

    public EnrollmentResponse rejectEnrollment(Long enrollmentId, Long registrarId, String remarks) {
        return runInTransaction(conn -> {
            lockEnrollment(conn, enrollmentId, EnrollmentStatus.PENDING);
            enrollmentDao.recordDecision(conn, enrollmentId, EnrollmentStatus.REJECTED, registrarId, remarks);
            return enrollmentId;
        });
    }

    public EnrollmentResponse approveDrop(Long enrollmentId, Long registrarId, String remarks) {
        return runInTransaction(conn -> {
            Enrollment enrollment = lockEnrollment(conn, enrollmentId, EnrollmentStatus.DROP_PENDING);
            courseDao.adjustSeatsTaken(conn, enrollment.courseId(), -1);
            enrollmentDao.recordDecision(conn, enrollmentId, EnrollmentStatus.DROPPED, registrarId, remarks);
            return enrollmentId;
        });
    }

    public EnrollmentResponse rejectDrop(Long enrollmentId, Long registrarId, String remarks) {
        return runInTransaction(conn -> {
            lockEnrollment(conn, enrollmentId, EnrollmentStatus.DROP_PENDING);
            // Denied drop request -> student stays enrolled.
            enrollmentDao.recordDecision(conn, enrollmentId, EnrollmentStatus.DROP_REJECTED, registrarId, remarks);
            return enrollmentId;
        });
    }

    // ------------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------------

    @FunctionalInterface
    private interface TxWork {
        Long run(Connection conn) throws SQLException;
    }

    /**
     * Runs `work` inside a single manually-managed transaction: one
     * Connection, autocommit off, commit on success / rollback on any
     * exception. All registrar decisions go through this so that the
     * "read current state -> validate -> mutate" sequence is atomic
     * under concurrent requests (see CourseDao.findByIdForUpdate).
     */
    private EnrollmentResponse runInTransaction(TxWork work) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Long enrollmentId = work.run(conn);
                conn.commit();
                return enrich(enrollmentDao.findById(enrollmentId).orElseThrow());
            } catch (RuntimeException | SQLException e) {
                conn.rollback();
                if (e instanceof RuntimeException re) throw re;
                throw new RuntimeException("Database error while processing decision", e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error opening transaction", e);
        }
    }

    private Enrollment lockEnrollment(Connection conn, Long id, EnrollmentStatus requiredStatus) throws SQLException {
        Enrollment enrollment = enrollmentDao.findByIdForUpdate(conn, id)
                .orElseThrow(() -> new ApiException(404, "Enrollment not found"));
        if (enrollment.status() != requiredStatus) {
            throw new ApiException(409, "This request is no longer " + requiredStatus + " (someone may have already acted on it)");
        }
        return enrollment;
    }

    private Course lockCourse(Connection conn, Long id) throws SQLException {
        return courseDao.findByIdForUpdate(conn, id)
                .orElseThrow(() -> new ApiException(404, "Course not found"));
    }

    private EnrollmentResponse enrich(Enrollment e) {
        User student = userDao.findById(e.studentId()).orElse(null);
        Course course = courseDao.findById(e.courseId()).orElse(null);
        Optional<User> decider = e.decidedBy() == null ? Optional.empty() : userDao.findById(e.decidedBy());

        return new EnrollmentResponse(
                e.id(),
                e.studentId(),
                student != null ? student.fullName() : "Unknown",
                e.courseId(),
                course != null ? course.code() : "Unknown",
                course != null ? course.title() : "Unknown",
                e.status().name(),
                e.requestedAt().format(TS_FMT),
                e.decidedAt() != null ? e.decidedAt().format(TS_FMT) : null,
                e.remarks() != null ? e.remarks() : (decider.map(d -> "Decided by " + d.fullName()).orElse(null))
        );
    }
}
