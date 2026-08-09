package com.courseregistration.dao;

import com.courseregistration.exception.ApiException;
import com.courseregistration.model.Quiz;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class QuizDao {

    private final DataSource dataSource;

    public QuizDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Quiz insert(Long courseId, String title, String description) {
        String sql = "INSERT INTO quizzes (course_id, title, description) VALUES (?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, courseId);
            ps.setString(2, title);
            ps.setString(3, description);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return findById(keys.getLong(1)).orElseThrow();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while creating quiz", e);
        }
    }

    public Optional<Quiz> findById(Long id) {
        String sql = "SELECT * FROM quizzes WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while finding quiz", e);
        }
    }

    public List<Quiz> findByCourse(Long courseId) {
        String sql = "SELECT * FROM quizzes WHERE course_id = ? ORDER BY id";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Quiz> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while listing quizzes", e);
        }
    }

    /** Cascades to questions/options/attempts/answers via ON DELETE CASCADE in the schema. */
    public void delete(Long id) {
        String sql = "DELETE FROM quizzes WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            int updated = ps.executeUpdate();
            if (updated == 0) throw new ApiException(404, "Quiz not found");
        } catch (SQLException e) {
            throw new RuntimeException("Database error while deleting quiz", e);
        }
    }

    private Quiz mapRow(ResultSet rs) throws SQLException {
        return new Quiz(
                rs.getLong("id"),
                rs.getLong("course_id"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getTimestamp("created_at").toLocalDateTime()
        );
    }
}
