package com.courseregistration.dao;

import com.courseregistration.model.Certificate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class CertificateDao {

    private final DataSource dataSource;

    public CertificateDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Certificate insert(Long studentId, Long courseId, String code) {
        String sql = "INSERT INTO certificates (student_id, course_id, certificate_code) VALUES (?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, studentId);
            ps.setLong(2, courseId);
            ps.setString(3, code);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return findById(keys.getLong(1)).orElseThrow();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while issuing certificate", e);
        }
    }

    public Optional<Certificate> findById(Long id) {
        return querySingle("SELECT * FROM certificates WHERE id = ?", id);
    }

    public Optional<Certificate> findByStudentAndCourse(Long studentId, Long courseId) {
        String sql = "SELECT * FROM certificates WHERE student_id = ? AND course_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, studentId);
            ps.setLong(2, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while finding certificate", e);
        }
    }

    /** Every certificate this student has earned, across all courses — used for badge computation. */
    public List<Certificate> findByStudent(Long studentId) {
        String sql = "SELECT * FROM certificates WHERE student_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Certificate> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while listing certificates", e);
        }
    }

    public Optional<Certificate> findByCode(String code) {
        String sql = "SELECT * FROM certificates WHERE certificate_code = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while verifying certificate", e);
        }
    }

    private Optional<Certificate> querySingle(String sql, Long id) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error while finding certificate", e);
        }
    }

    private Certificate mapRow(ResultSet rs) throws SQLException {
        return new Certificate(
                rs.getLong("id"),
                rs.getLong("student_id"),
                rs.getLong("course_id"),
                rs.getString("certificate_code"),
                rs.getTimestamp("issued_at").toLocalDateTime()
        );
    }
}
