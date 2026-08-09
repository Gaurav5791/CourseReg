package com.courseregistration.dao;

import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;

@Repository
public class ContentProgressDao {

    private final DataSource dataSource;

    public ContentProgressDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** Idempotent — marking something complete twice is a no-op, not an error. */
    public void markComplete(Long studentId, Long contentId) {
        String sql = "INSERT IGNORE INTO content_progress (student_id, content_id) VALUES (?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, studentId);
            ps.setLong(2, contentId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Database error while marking content complete", e);
        }
    }

    /** IDs of every content item this student has completed, across ALL courses (caller filters by course). */
    public Set<Long> findCompletedContentIds(Long studentId) {
        String sql = "SELECT content_id FROM content_progress WHERE student_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                Set<Long> ids = new HashSet<>();
                while (rs.next()) ids.add(rs.getLong("content_id"));
                return ids;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while reading content progress", e);
        }
    }

    /** How many of a course's lessons this student has completed — one query via a join, not N+1. */
    public int countCompletedForCourse(Long studentId, Long courseId) {
        String sql = "SELECT COUNT(*) FROM content_progress cp " +
                "JOIN course_contents cc ON cp.content_id = cc.id " +
                "WHERE cp.student_id = ? AND cc.course_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, studentId);
            ps.setLong(2, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while counting completed content", e);
        }
    }
}
