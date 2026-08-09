package com.courseregistration.dao;

import com.courseregistration.exception.ApiException;
import com.courseregistration.model.ContentType;
import com.courseregistration.model.CourseContent;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class CourseContentDao {

    private final DataSource dataSource;

    public CourseContentDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<CourseContent> findByCourse(Long courseId) {
        String sql = "SELECT * FROM course_contents WHERE course_id = ? ORDER BY order_index, id";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                List<CourseContent> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while listing course content", e);
        }
    }

    public Optional<CourseContent> findById(Long id) {
        String sql = "SELECT * FROM course_contents WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while finding course content", e);
        }
    }

    public CourseContent insert(Long courseId, String title, ContentType type, String body, String externalUrl, int orderIndex) {
        String sql = "INSERT INTO course_contents (course_id, title, content_type, body, external_url, order_index) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, courseId);
            ps.setString(2, title);
            ps.setString(3, type.name());
            ps.setString(4, body);
            ps.setString(5, externalUrl);
            ps.setInt(6, orderIndex);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return findById(keys.getLong(1)).orElseThrow();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while creating course content", e);
        }
    }

    public CourseContent update(Long id, String title, ContentType type, String body, String externalUrl, int orderIndex) {
        String sql = "UPDATE course_contents SET title = ?, content_type = ?, body = ?, external_url = ?, order_index = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, type.name());
            ps.setString(3, body);
            ps.setString(4, externalUrl);
            ps.setInt(5, orderIndex);
            ps.setLong(6, id);
            int updated = ps.executeUpdate();
            if (updated == 0) throw new ApiException(404, "Content item not found");
            return findById(id).orElseThrow();
        } catch (SQLException e) {
            throw new RuntimeException("Database error while updating course content", e);
        }
    }

    public void delete(Long id) {
        String sql = "DELETE FROM course_contents WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            int updated = ps.executeUpdate();
            if (updated == 0) throw new ApiException(404, "Content item not found");
        } catch (SQLException e) {
            throw new RuntimeException("Database error while deleting course content", e);
        }
    }

    private CourseContent mapRow(ResultSet rs) throws SQLException {
        return new CourseContent(
                rs.getLong("id"),
                rs.getLong("course_id"),
                rs.getString("title"),
                ContentType.valueOf(rs.getString("content_type")),
                rs.getString("body"),
                rs.getString("external_url"),
                rs.getInt("order_index"),
                rs.getTimestamp("created_at").toLocalDateTime()
        );
    }
}
