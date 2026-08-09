package com.courseregistration.dao;

import com.courseregistration.model.QuizOption;
import com.courseregistration.model.QuizQuestion;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class QuizQuestionDao {

    private final DataSource dataSource;

    public QuizQuestionDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<QuizQuestion> findByQuiz(Long quizId) {
        String sql = "SELECT * FROM quiz_questions WHERE quiz_id = ? ORDER BY order_index, id";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, quizId);
            try (ResultSet rs = ps.executeQuery()) {
                List<QuizQuestion> list = new ArrayList<>();
                while (rs.next()) list.add(mapQuestionRow(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while listing quiz questions", e);
        }
    }

    public List<QuizOption> findOptionsByQuestion(Long questionId) {
        String sql = "SELECT * FROM quiz_options WHERE question_id = ? ORDER BY order_index, id";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, questionId);
            try (ResultSet rs = ps.executeQuery()) {
                List<QuizOption> list = new ArrayList<>();
                while (rs.next()) list.add(mapOptionRow(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while listing quiz options", e);
        }
    }

    /** Inserts a question row inside a caller-managed transaction (see QuizService.addQuestion). */
    public Long insertQuestion(Connection conn, Long quizId, String questionText, int orderIndex) throws SQLException {
        String sql = "INSERT INTO quiz_questions (quiz_id, question_text, order_index) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, quizId);
            ps.setString(2, questionText);
            ps.setInt(3, orderIndex);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    /** Inserts one option row inside the same transaction as insertQuestion. */
    public void insertOption(Connection conn, Long questionId, String optionText, boolean correct, int orderIndex) throws SQLException {
        String sql = "INSERT INTO quiz_options (question_id, option_text, is_correct, order_index) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, questionId);
            ps.setString(2, optionText);
            ps.setBoolean(3, correct);
            ps.setInt(4, orderIndex);
            ps.executeUpdate();
        }
    }

    /** Cascades to its options via ON DELETE CASCADE. */
    public void deleteQuestion(Long questionId) {
        String sql = "DELETE FROM quiz_questions WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, questionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Database error while deleting question", e);
        }
    }

    private QuizQuestion mapQuestionRow(ResultSet rs) throws SQLException {
        return new QuizQuestion(
                rs.getLong("id"),
                rs.getLong("quiz_id"),
                rs.getString("question_text"),
                rs.getInt("order_index")
        );
    }

    private QuizOption mapOptionRow(ResultSet rs) throws SQLException {
        return new QuizOption(
                rs.getLong("id"),
                rs.getLong("question_id"),
                rs.getString("option_text"),
                rs.getBoolean("is_correct"),
                rs.getInt("order_index")
        );
    }
}
