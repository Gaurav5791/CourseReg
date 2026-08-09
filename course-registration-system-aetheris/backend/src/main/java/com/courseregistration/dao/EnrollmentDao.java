package com.courseregistration.dao;

import com.courseregistration.model.Enrollment;
import com.courseregistration.model.EnrollmentStatus;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class EnrollmentDao {

    private final DataSource dataSource;

    public EnrollmentDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Enrollment insertPending(Long studentId, Long courseId) {
        String sql = "INSERT INTO enrollments (student_id, course_id, status) VALUES (?, ?, 'PENDING')";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, studentId);
            ps.setLong(2, courseId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return findById(keys.getLong(1)).orElseThrow();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while creating enrollment request", e);
        }
    }

    public Optional<Enrollment> findById(Long id) {
        String sql = "SELECT * FROM enrollments WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while finding enrollment", e);
        }
    }

    /** Row-locking read used inside a transaction (see EnrollmentService approve/reject). */
    public Optional<Enrollment> findByIdForUpdate(Connection conn, Long id) throws SQLException {
        String sql = "SELECT * FROM enrollments WHERE id = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    /** Any row for this student+course that isn't in a terminal state (REJECTED/DROPPED/DROP_REJECTED). */
    public Optional<Enrollment> findActiveByStudentAndCourse(Long studentId, Long courseId) {
        String sql = "SELECT * FROM enrollments WHERE student_id = ? AND course_id = ? " +
                "AND status IN ('PENDING','APPROVED','DROP_PENDING')";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, studentId);
            ps.setLong(2, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while checking existing enrollment", e);
        }
    }

    /** Used to gate course content access — only APPROVED students can view lessons. */
    public boolean isApprovedForCourse(Long studentId, Long courseId) {
        String sql = "SELECT 1 FROM enrollments WHERE student_id = ? AND course_id = ? AND status = 'APPROVED'";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, studentId);
            ps.setLong(2, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while checking course approval", e);
        }
    }

    public List<Enrollment> findByStudent(Long studentId) {
        String sql = "SELECT * FROM enrollments WHERE student_id = ? ORDER BY requested_at DESC";
        return queryList(sql, studentId);
    }

    /** All APPROVED enrollments in a course — used by the admin analytics view. */
    public List<Enrollment> findApprovedByCourse(Long courseId) {
        String sql = "SELECT * FROM enrollments WHERE course_id = ? AND status = 'APPROVED' ORDER BY requested_at";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Enrollment> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while listing approved students for course", e);
        }
    }

    public List<Enrollment> findByStatus(EnrollmentStatus status) {
        String sql = "SELECT * FROM enrollments WHERE status = ? ORDER BY requested_at";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                List<Enrollment> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while listing enrollments by status", e);
        }
    }

    private List<Enrollment> queryList(String sql, Long id) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                List<Enrollment> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while listing enrollments", e);
        }
    }

    /** Simple, non-transactional status update — used for the student's own drop request (PENDING->none, APPROVED->DROP_PENDING). */
    public void updateStatusSimple(Long id, EnrollmentStatus status) {
        String sql = "UPDATE enrollments SET status = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Database error while updating enrollment status", e);
        }
    }

    /**
     * All non-terminal enrollments (PENDING/APPROVED/DROP_PENDING) for a
     * course, locked FOR UPDATE. Used when a course is removed so those
     * rows can be auto-dropped in the same transaction without a
     * separate lookup per status.
     */
    public List<Enrollment> findActiveByCourseForUpdate(Connection conn, Long courseId) throws SQLException {
        String sql = "SELECT * FROM enrollments WHERE course_id = ? " +
                "AND status IN ('PENDING','APPROVED','DROP_PENDING') FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Enrollment> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        }
    }

    /** Registrar decision — runs inside the same transaction/connection as the course seat-count update. */
    public void recordDecision(Connection conn, Long id, EnrollmentStatus status, Long decidedBy, String remarks) throws SQLException {
        String sql = "UPDATE enrollments SET status = ?, decided_at = NOW(), decided_by = ?, remarks = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setLong(2, decidedBy);
            ps.setString(3, remarks);
            ps.setLong(4, id);
            ps.executeUpdate();
        }
    }

    private Enrollment mapRow(ResultSet rs) throws SQLException {
        // wasNull() reflects only the MOST RECENT column read, so we must
        // read decided_by and check wasNull() immediately, before any other
        // rs.getX() call — otherwise it'd report the null-ness of whatever
        // column we read next instead.
        long decidedByRaw = rs.getLong("decided_by");
        Long decidedBy = rs.wasNull() ? null : decidedByRaw;

        Timestamp decidedAtTs = rs.getTimestamp("decided_at");
        LocalDateTime decidedAt = (decidedAtTs == null) ? null : decidedAtTs.toLocalDateTime();

        return new Enrollment(
                rs.getLong("id"),
                rs.getLong("student_id"),
                rs.getLong("course_id"),
                EnrollmentStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("requested_at").toLocalDateTime(),
                decidedAt,
                decidedBy,
                rs.getString("remarks")
        );
    }
}
