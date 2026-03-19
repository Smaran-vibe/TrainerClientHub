package com.trainerclienthub.DAO;

import com.trainerclienthub.db.DatabaseConnection;
import com.trainerclienthub.db.DatabaseException;
import com.trainerclienthub.model.Report;
import com.trainerclienthub.model.ReportType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReportDAO {


    private static final String INSERT =
            "INSERT INTO report (generated_by, report_type, generated_date, content) VALUES (?, ?, ?, ?)";

    private static final String SELECT_BY_ID =
            "SELECT * FROM report WHERE report_id = ?";

    private static final String SELECT_BY_TRAINER =
            "SELECT * FROM report WHERE generated_by = ? ORDER BY generated_date DESC";

    private static final String SELECT_BY_TYPE =
            "SELECT * FROM report WHERE report_type = ? ORDER BY generated_date DESC";

    private static final String SELECT_BY_DATE_RANGE =
            "SELECT * FROM report WHERE generated_date BETWEEN ? AND ? ORDER BY generated_date DESC";

    private static final String SELECT_ALL =
            "SELECT * FROM report ORDER BY generated_date DESC";

    private static final String DELETE =
            "DELETE FROM report WHERE report_id = ?";


    public void insert(Report report) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, report.getGeneratedBy());
            ps.setString(2, report.getReportType().name());
            ps.setTimestamp(3, Timestamp.valueOf(report.getGeneratedDate()));
            ps.setString(4, report.getContent());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) report.setReportId(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert report.", e);
        }
    }


    public Optional<Report> findById(int reportId) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_ID)) {

            ps.setInt(1, reportId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find report id: " + reportId, e);
        }
    }

    public List<Report> findByTrainer(int trainerId) {
        List<Report> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_TRAINER)) {

            ps.setInt(1, trainerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch reports for trainer id: " + trainerId, e);
        }
        return list;
    }

    public List<Report> findByType(ReportType type) {
        List<Report> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_TYPE)) {

            ps.setString(1, type.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch reports of type: " + type, e);
        }
        return list;
    }

    public List<Report> findByDateRange(Timestamp from, Timestamp to) {
        List<Report> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_DATE_RANGE)) {

            ps.setTimestamp(1, from);
            ps.setTimestamp(2, to);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch reports in date range.", e);
        }
        return list;
    }

    public List<Report> findAll() {
        List<Report> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch all reports.", e);
        }
        return list;
    }


    /** Reports are immutable once generated — no update method is provided. */
    public void delete(int reportId) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE)) {

            ps.setInt(1, reportId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete report id: " + reportId, e);
        }
    }


    private Report map(ResultSet rs) throws SQLException {
        return new Report(
                rs.getInt("report_id"),
                rs.getInt("generated_by"),
                ReportType.valueOf(rs.getString("report_type").toUpperCase()),
                rs.getTimestamp("generated_date").toLocalDateTime(),
                rs.getString("content")
        );
    }
}
