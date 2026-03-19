package com.trainerclienthub.DAO;

import com.trainerclienthub.db.DatabaseConnection;
import com.trainerclienthub.db.DatabaseException;
import com.trainerclienthub.model.Session;
import com.trainerclienthub.model.SessionStatus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SessionDAO {


    private static final String INSERT =
            "INSERT INTO session (client_id, trainer_id, session_date, session_time, status, notes) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SELECT_BY_ID =
            "SELECT * FROM session WHERE session_id = ?";

    private static final String SELECT_BY_CLIENT =
            "SELECT * FROM session WHERE client_id = ? ORDER BY session_date DESC, session_time DESC";

    private static final String SELECT_BY_TRAINER =
            "SELECT * FROM session WHERE trainer_id = ? ORDER BY session_date DESC, session_time DESC";

    private static final String SELECT_BY_DATE =
            "SELECT * FROM session WHERE session_date = ? ORDER BY session_time";

    private static final String SELECT_BY_CLIENT_AND_STATUS =
            "SELECT * FROM session WHERE client_id = ? AND status = ? ORDER BY session_date DESC";

    private static final String SELECT_UPCOMING_BY_CLIENT =
            "SELECT * FROM session WHERE client_id = ? AND status = 'SCHEDULED' AND session_date >= CURDATE() " +
            "ORDER BY session_date, session_time";

    private static final String SELECT_ALL =
            "SELECT * FROM session ORDER BY session_date DESC, session_time DESC";

    private static final String UPDATE =
            "UPDATE session SET client_id = ?, trainer_id = ?, session_date = ?, session_time = ?, " +
            "status = ?, notes = ? WHERE session_id = ?";

    private static final String UPDATE_STATUS =
            "UPDATE session SET status = ? WHERE session_id = ?";

    private static final String DELETE =
            "DELETE FROM session WHERE session_id = ?";


    public void insert(Session session) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, session.getClientId());
            ps.setInt(2, session.getTrainerId());
            ps.setDate(3, Date.valueOf(session.getSessionDate()));
            ps.setTime(4, Time.valueOf(session.getSessionTime()));
            ps.setString(5, session.getStatus().name());
            ps.setString(6, session.getNotes());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) session.setSessionId(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert session for client id: " + session.getClientId(), e);
        }
    }


    public Optional<Session> findById(int sessionId) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_ID)) {

            ps.setInt(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find session id: " + sessionId, e);
        }
    }

    public List<Session> findByClient(int clientId) {
        List<Session> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_CLIENT)) {

            ps.setInt(1, clientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch sessions for client id: " + clientId, e);
        }
        return list;
    }

    public List<Session> findByTrainer(int trainerId) {
        List<Session> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_TRAINER)) {

            ps.setInt(1, trainerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch sessions for trainer id: " + trainerId, e);
        }
        return list;
    }

    public List<Session> findByDate(Date date) {
        List<Session> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_DATE)) {

            ps.setDate(1, date);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch sessions for date: " + date, e);
        }
        return list;
    }

    public List<Session> findByClientAndStatus(int clientId, SessionStatus status) {
        List<Session> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_CLIENT_AND_STATUS)) {

            ps.setInt(1, clientId);
            ps.setString(2, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch sessions for client id " + clientId + " with status " + status, e);
        }
        return list;
    }

    /** Returns upcoming SCHEDULED sessions for a client from today onwards. */
    public List<Session> findUpcomingByClient(int clientId) {
        List<Session> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_UPCOMING_BY_CLIENT)) {

            ps.setInt(1, clientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch upcoming sessions for client id: " + clientId, e);
        }
        return list;
    }

    public List<Session> findAll() {
        List<Session> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch all sessions.", e);
        }
        return list;
    }


    public void update(Session session) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE)) {

            ps.setInt(1, session.getClientId());
            ps.setInt(2, session.getTrainerId());
            ps.setDate(3, Date.valueOf(session.getSessionDate()));
            ps.setTime(4, Time.valueOf(session.getSessionTime()));
            ps.setString(5, session.getStatus().name());
            ps.setString(6, session.getNotes());
            ps.setInt(7, session.getSessionId());
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Failed to update session id: " + session.getSessionId(), e);
        }
    }

    /** Updates only the status column — used for complete/cancel/no-show transitions. */
    public void updateStatus(int sessionId, SessionStatus status) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_STATUS)) {

            ps.setString(1, status.name());
            ps.setInt(2, sessionId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Failed to update status for session id: " + sessionId, e);
        }
    }


    public void delete(int sessionId) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE)) {

            ps.setInt(1, sessionId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete session id: " + sessionId, e);
        }
    }


    private Session map(ResultSet rs) throws SQLException {
        return new Session(
                rs.getInt("session_id"),
                rs.getInt("client_id"),
                rs.getInt("trainer_id"),
                rs.getDate("session_date").toLocalDate(),
                rs.getTime("session_time").toLocalTime(),
                SessionStatus.valueOf(rs.getString("status").toUpperCase()),
                rs.getString("notes")
        );
    }
}
