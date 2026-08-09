package com.courseregistration.dao;

import com.courseregistration.exception.ApiException;
import com.courseregistration.model.Course;
import com.courseregistration.model.CourseStatus;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class CourseDao {

    private final DataSource dataSource;

    public CourseDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Course> findAllActive(String keyword, String semester) {
        StringBuilder sql = new StringBuilder("SELECT * FROM courses WHERE status = 'ACTIVE'");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (code LIKE ? OR title LIKE ? OR instructor_name LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (semester != null && !semester.isBlank()) {
            sql.append(" AND semester = ?");
            params.add(semester.trim());
        }
        sql.append(" ORDER BY code");

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Course> courses = new ArrayList<>();
                while (rs.next()) courses.add(mapRow(rs));
                return courses;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while searching courses", e);
        }
    }

    /** All courses regardless of status — used by admin/registrar views. */
    public List<Course> findAll() {
        String sql = "SELECT * FROM courses ORDER BY code";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Course> courses = new ArrayList<>();
            while (rs.next()) courses.add(mapRow(rs));
            return courses;
        } catch (SQLException e) {
            throw new RuntimeException("Database error while listing courses", e);
        }
    }

    public Optional<Course> findById(Long id) {
        String sql = "SELECT * FROM courses WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while finding course", e);
        }
    }

    /**
     * Reads the course row WITH a row lock (SELECT ... FOR UPDATE), using
     * a Connection that the caller already has open in a transaction.
     * This is what stops two registrar approvals (or a race between an
     * approval and a drop) from both reading "1 seat left" and both
     * succeeding — the second one blocks until the first commits, then
     * sees the updated seat count. Without this, courses could silently
     * overbook under concurrent requests.
     */
    public Optional<Course> findByIdForUpdate(Connection conn, Long id) throws SQLException {
        String sql = "SELECT * FROM courses WHERE id = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    /** Adjusts seats_taken by delta (+1 on approve, -1 on drop). Must run inside the same transaction as the lock above. */
    public void adjustSeatsTaken(Connection conn, Long courseId, int delta) throws SQLException {
        String sql = "UPDATE courses SET seats_taken = seats_taken + ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, delta);
            ps.setLong(2, courseId);
            ps.executeUpdate();
        }
    }

    public Course insert(Course c) {
        String sql = "INSERT INTO courses (code, title, description, credits, instructor_name, day_of_week, " +
                "start_time, end_time, semester, capacity) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.code());
            ps.setString(2, c.title());
            ps.setString(3, c.description());
            ps.setInt(4, c.credits());
            ps.setString(5, c.instructorName());
            ps.setString(6, c.dayOfWeek());
            ps.setTime(7, Time.valueOf(c.startTime()));
            ps.setTime(8, Time.valueOf(c.endTime()));
            ps.setString(9, c.semester());
            ps.setInt(10, c.capacity());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return findById(keys.getLong(1)).orElseThrow();
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new ApiException(409, "A course with that code already exists");
        } catch (SQLException e) {
            throw new RuntimeException("Database error while creating course", e);
        }
    }

    public Course update(Long id, Course c) {
        String sql = "UPDATE courses SET title = ?, description = ?, credits = ?, instructor_name = ?, " +
                "day_of_week = ?, start_time = ?, end_time = ?, semester = ?, capacity = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.title());
            ps.setString(2, c.description());
            ps.setInt(3, c.credits());
            ps.setString(4, c.instructorName());
            ps.setString(5, c.dayOfWeek());
            ps.setTime(6, Time.valueOf(c.startTime()));
            ps.setTime(7, Time.valueOf(c.endTime()));
            ps.setString(8, c.semester());
            ps.setInt(9, c.capacity());
            ps.setLong(10, id);
            int updated = ps.executeUpdate();
            if (updated == 0) throw new ApiException(404, "Course not found");
            return findById(id).orElseThrow();
        } catch (SQLException e) {
            throw new RuntimeException("Database error while updating course", e);
        }
    }

    /** Soft-delete. Only ever called from the registrar's remove-course endpoint. */
    public void markRemoved(Long id) {
        String sql = "UPDATE courses SET status = 'REMOVED' WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            int updated = ps.executeUpdate();
            if (updated == 0) throw new ApiException(404, "Course not found");
        } catch (SQLException e) {
            throw new RuntimeException("Database error while removing course", e);
        }
    }

    private Course mapRow(ResultSet rs) throws SQLException {
        return new Course(
                rs.getLong("id"),
                rs.getString("code"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getInt("credits"),
                rs.getString("instructor_name"),
                rs.getString("day_of_week"),
                rs.getTime("start_time").toLocalTime(),
                rs.getTime("end_time").toLocalTime(),
                rs.getString("semester"),
                rs.getInt("capacity"),
                rs.getInt("seats_taken"),
                CourseStatus.valueOf(rs.getString("status"))
        );
    }
}
