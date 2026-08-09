package com.courseregistration.dao;

import com.courseregistration.exception.ApiException;
import com.courseregistration.model.SideQuest;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public class SideQuestDao {

    private final DataSource dataSource;

    public SideQuestDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public SideQuest insert(Long courseId, String title, String description, int points) {
        String sql = "INSERT INTO side_quests (course_id, title, description, points) VALUES (?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, courseId);
            ps.setString(2, title);
            ps.setString(3, description);
            ps.setInt(4, points);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return findById(keys.getLong(1)).orElseThrow();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while creating side quest", e);
        }
    }

    public Optional<SideQuest> findById(Long id) {
        String sql = "SELECT * FROM side_quests WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while finding side quest", e);
        }
    }

    public List<SideQuest> findByCourse(Long courseId) {
        String sql = "SELECT * FROM side_quests WHERE course_id = ? ORDER BY id";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                List<SideQuest> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while listing side quests", e);
        }
    }

    /** Cascades to side_quest_completions via ON DELETE CASCADE. */
    public void delete(Long id) {
        String sql = "DELETE FROM side_quests WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            int updated = ps.executeUpdate();
            if (updated == 0) throw new ApiException(404, "Quest not found");
        } catch (SQLException e) {
            throw new RuntimeException("Database error while deleting side quest", e);
        }
    }

    /** Idempotent — completing the same quest twice is a no-op, not an error. */
    public void markComplete(Long studentId, Long questId) {
        String sql = "INSERT IGNORE INTO side_quest_completions (student_id, quest_id) VALUES (?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, studentId);
            ps.setLong(2, questId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Database error while completing side quest", e);
        }
    }

    /** All quest IDs this student has completed, across every course. */
    public Set<Long> findCompletedQuestIds(Long studentId) {
        String sql = "SELECT quest_id FROM side_quest_completions WHERE student_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                Set<Long> ids = new HashSet<>();
                while (rs.next()) ids.add(rs.getLong("quest_id"));
                return ids;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while reading quest completions", e);
        }
    }

    /** Total points earned from quests in one course — one query via a join, not N+1. */
    public int sumPointsForCourse(Long studentId, Long courseId) {
        String sql = "SELECT COALESCE(SUM(sq.points), 0) FROM side_quest_completions qc " +
                "JOIN side_quests sq ON qc.quest_id = sq.id " +
                "WHERE qc.student_id = ? AND sq.course_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, studentId);
            ps.setLong(2, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while summing quest points", e);
        }
    }

    private SideQuest mapRow(ResultSet rs) throws SQLException {
        return new SideQuest(
                rs.getLong("id"),
                rs.getLong("course_id"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getInt("points"),
                rs.getTimestamp("created_at").toLocalDateTime()
        );
    }
}
