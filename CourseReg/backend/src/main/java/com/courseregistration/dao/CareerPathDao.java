package com.courseregistration.dao;

import com.courseregistration.exception.ApiException;
import com.courseregistration.model.CareerPath;
import com.courseregistration.model.Course;
import com.courseregistration.model.CourseStatus;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class CareerPathDao {

    private final DataSource dataSource;

    public CareerPathDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public CareerPath insert(String name, String description) {
        String sql = "INSERT INTO career_paths (name, description) VALUES (?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, description);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return findById(keys.getLong(1)).orElseThrow();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while creating career path", e);
        }
    }

    public Optional<CareerPath> findById(Long id) {
        String sql = "SELECT * FROM career_paths WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while finding career path", e);
        }
    }

    public List<CareerPath> findAll() {
        String sql = "SELECT * FROM career_paths ORDER BY name";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<CareerPath> list = new ArrayList<>();
            while (rs.next()) list.add(mapRow(rs));
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Database error while listing career paths", e);
        }
    }

    /** Cascades to career_path_courses via ON DELETE CASCADE. */
    public void delete(Long id) {
        String sql = "DELETE FROM career_paths WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            int updated = ps.executeUpdate();
            if (updated == 0) throw new ApiException(404, "Career path not found");
        } catch (SQLException e) {
            throw new RuntimeException("Database error while deleting career path", e);
        }
    }

    /** Idempotent — adding the same course twice just updates its order instead of erroring. */
    public void addCourse(Long careerPathId, Long courseId, int orderIndex) {
        String sql = "INSERT INTO career_path_courses (career_path_id, course_id, order_index) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE order_index = VALUES(order_index)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, careerPathId);
            ps.setLong(2, courseId);
            ps.setInt(3, orderIndex);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Database error while adding course to path", e);
        }
    }

    public void removeCourse(Long careerPathId, Long courseId) {
        String sql = "DELETE FROM career_path_courses WHERE career_path_id = ? AND course_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, careerPathId);
            ps.setLong(2, courseId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Database error while removing course from path", e);
        }
    }

    public int countCoursesForPath(Long careerPathId) {
        String sql = "SELECT COUNT(*) FROM career_path_courses WHERE career_path_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, careerPathId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while counting path courses", e);
        }
    }

    /** The courses in a path, in order, with full course details (reuses the courses table's own columns). */
    public List<Course> findCoursesForPath(Long careerPathId) {
        String sql = "SELECT c.* FROM courses c " +
                "JOIN career_path_courses pc ON c.id = pc.course_id " +
                "WHERE pc.career_path_id = ? ORDER BY pc.order_index";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, careerPathId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Course> list = new ArrayList<>();
                while (rs.next()) list.add(mapCourseRow(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while listing path courses", e);
        }
    }

    private CareerPath mapRow(ResultSet rs) throws SQLException {
        return new CareerPath(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getTimestamp("created_at").toLocalDateTime()
        );
    }

    private Course mapCourseRow(ResultSet rs) throws SQLException {
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
