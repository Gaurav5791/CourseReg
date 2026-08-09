package com.courseregistration.dao;

import com.courseregistration.model.QuizAttempt;
import com.courseregistration.model.QuizAttemptAnswer;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class QuizAttemptDao {

    private final DataSource dataSource;

    public QuizAttemptDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** Inserts the attempt row inside a caller-managed transaction (see QuizService.submitAttempt). */
    public Long insertAttempt(Connection conn, Long quizId, Long studentId, int score, int totalQuestions) throws SQLException {
        String sql = "INSERT INTO quiz_attempts (quiz_id, student_id, score, total_questions) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, quizId);
            ps.setLong(2, studentId);
            ps.setInt(3, score);
            ps.setInt(4, totalQuestions);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    /** Inserts one answer row inside the same transaction as insertAttempt. selectedOptionId may be null (unanswered). */
    public void insertAnswer(Connection conn, Long attemptId, Long questionId, Long selectedOptionId, boolean wasCorrect) throws SQLException {
        String sql = "INSERT INTO quiz_attempt_answers (attempt_id, question_id, selected_option_id, was_correct) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, attemptId);
            ps.setLong(2, questionId);
            if (selectedOptionId != null) ps.setLong(3, selectedOptionId); else ps.setNull(3, Types.BIGINT);
            ps.setBoolean(4, wasCorrect);
            ps.executeUpdate();
        }
    }

    public Optional<QuizAttempt> findById(Long id) {
        String sql = "SELECT * FROM quiz_attempts WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapAttemptRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while finding quiz attempt", e);
        }
    }

    public List<QuizAttempt> findByStudentAndQuiz(Long studentId, Long quizId) {
        String sql = "SELECT * FROM quiz_attempts WHERE student_id = ? AND quiz_id = ? ORDER BY submitted_at DESC";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, studentId);
            ps.setLong(2, quizId);
            try (ResultSet rs = ps.executeQuery()) {
                List<QuizAttempt> list = new ArrayList<>();
                while (rs.next()) list.add(mapAttemptRow(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while listing quiz attempts", e);
        }
    }

    public List<QuizAttemptAnswer> findAnswersByAttempt(Long attemptId) {
        String sql = "SELECT * FROM quiz_attempt_answers WHERE attempt_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, attemptId);
            try (ResultSet rs = ps.executeQuery()) {
                List<QuizAttemptAnswer> list = new ArrayList<>();
                while (rs.next()) list.add(mapAnswerRow(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while listing quiz answers", e);
        }
    }

    private QuizAttempt mapAttemptRow(ResultSet rs) throws SQLException {
        return new QuizAttempt(
                rs.getLong("id"),
                rs.getLong("quiz_id"),
                rs.getLong("student_id"),
                rs.getInt("score"),
                rs.getInt("total_questions"),
                rs.getTimestamp("submitted_at").toLocalDateTime()
        );
    }

    private QuizAttemptAnswer mapAnswerRow(ResultSet rs) throws SQLException {
        long selectedRaw = rs.getLong("selected_option_id");
        Long selectedOptionId = rs.wasNull() ? null : selectedRaw;
        return new QuizAttemptAnswer(
                rs.getLong("id"),
                rs.getLong("attempt_id"),
                rs.getLong("question_id"),
                selectedOptionId,
                rs.getBoolean("was_correct")
        );
    }
}
