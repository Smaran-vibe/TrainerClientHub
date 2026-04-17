package com.trainerclienthub.DAO;

import com.trainerclienthub.db.DatabaseConnection;
import com.trainerclienthub.db.DatabaseException;
import com.trainerclienthub.model.Session;
import com.trainerclienthub.model.SessionStatus;
import com.trainerclienthub.model.TrainerRole;
import com.trainerclienthub.util.SessionManager;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SessionDAO {


    private static final String INSERT =
            "INSERT INTO session (client_id, trainer_id, session_date, session_time, status, notes) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SESSION_SELECT_BASE =
            "SELECT s.*, t.name AS trainer_name " +
                    "FROM session s " +
                    "JOIN trainer t ON s.trainer_id = t.trainer_id ";

    private static final String SELECT_BY_ID =
            SESSION_SELECT_BASE + "WHERE s.session_id = ?";

    private static final String SELECT_BY_CLIENT =
            SESSION_SELECT_BASE + "WHERE s.client_id = ? ORDER BY s.session_date DESC, s.session_time DESC";

    private static final String SELECT_BY_TRAINER =
            SESSION_SELECT_BASE + "WHERE s.trainer_id = ? ORDER BY s.session_date DESC, s.session_time DESC";

    private static final String SELECT_BY_DATE =
            SESSION_SELECT_BASE + "WHERE s.session_date = ? ORDER BY s.session_time";

    private static final String SELECT_BY_CLIENT_AND_STATUS =
            SESSION_SELECT_BASE + "WHERE s.client_id = ? AND s.status = ? ORDER BY s.session_date DESC";

    private static final String SELECT_UPCOMING_BY_CLIENT =
            SESSION_SELECT_BASE + "WHERE s.client_id = ? AND s.status = 'SCHEDULED' AND s.session_date >= CURDATE() " +
                    "ORDER BY s.session_date, s.session_time";

    private static final String SELECT_ALL =
            SESSION_SELECT_BASE + "ORDER BY s.session_date DESC, s.session_time DESC";

    private static final String SELECT_ALL_BY_TRAINER_CLIENTS =
            SESSION_SELECT_BASE +
                    "JOIN client c ON s.client_id = c.client_id " +
                    "WHERE c.trainer_id = ? ORDER BY s.session_date DESC, s.session_time DESC";

    private static final String UPDATE =
            "UPDATE session SET client_id = ?, trainer_id = ?, session_date = ?, session_time = ?, " +
                    "status = ?, notes = ? WHERE session_id = ?";

    private static final String UPDATE_STATUS =
            "UPDATE session SET status = ? WHERE session_id = ?";

    private static final String DELETE =
            "DELETE FROM session WHERE session_id = ?";

    private static final String CHECK_DUPLICATE_TIME_SLOT =
            "SELECT COUNT(1) FROM session " +
                    "WHERE client_id = ? AND trainer_id = ? AND session_date = ? AND session_time = ?";


    private static final String SELECT_MOST_ACTIVE_CLIENTS =
            "SELECT c.name AS client_name, COUNT(*) AS completed_sessions " +
                    "FROM session s " +
                    "JOIN client c ON s.client_id = c.client_id " +
                    "WHERE s.status = 'COMPLETED' AND s.session_date BETWEEN ? AND ? " +
                    "GROUP BY c.client_id, c.name " +
                    "ORDER BY completed_sessions DESC, c.name ASC " +
                    "LIMIT ?";

    private static final String SELECT_MOST_ACTIVE_CLIENTS_BY_TRAINER =
            "SELECT c.name AS client_name, COUNT(*) AS completed_sessions " +
                    "FROM session s " +
                    "JOIN client c ON s.client_id = c.client_id " +
                    "WHERE s.status = 'COMPLETED' AND s.session_date BETWEEN ? AND ? AND s.trainer_id = ? " +
                    "GROUP BY c.client_id, c.name " +
                    "ORDER BY completed_sessions DESC, c.name ASC " +
                    "LIMIT ?";


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

    public List<Map.Entry<String, Integer>> findMostActiveClients(LocalDate from, LocalDate to, int limit) {
        return findMostActiveClients(from, to, limit, null);
    }


    public List<Map.Entry<String, Integer>> findMostActiveClients(LocalDate from, LocalDate to, int limit, Integer trainerId) {
        List<Map.Entry<String, Integer>> list = new ArrayList<>();
        boolean filterByTrainer = trainerId != null;

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(filterByTrainer ? SELECT_MOST_ACTIVE_CLIENTS_BY_TRAINER : SELECT_MOST_ACTIVE_CLIENTS)) {

            if (filterByTrainer) {
                ps.setDate(1, Date.valueOf(from));
                ps.setDate(2, Date.valueOf(to));
                ps.setInt(3, trainerId);
                ps.setInt(4, limit);
            } else {
                ps.setDate(1, Date.valueOf(from));
                ps.setDate(2, Date.valueOf(to));
                ps.setInt(3, limit);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(Map.entry(rs.getString("client_name"), rs.getInt("completed_sessions")));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch most active clients.", e);
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
        var role = SessionManager.getInstance().getRole();
        if (role == TrainerRole.TRAINER) {
            var trainer = SessionManager.getInstance().getCurrentTrainer();
            if (trainer == null) return new ArrayList<>();
            List<Session> list = new ArrayList<>();
            try (Connection conn = DatabaseConnection.getInstance().getConnection();
                 PreparedStatement ps = conn.prepareStatement(SELECT_ALL_BY_TRAINER_CLIENTS)) {
                ps.setInt(1, trainer.getTrainerId());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) list.add(map(rs));
                }
            } catch (SQLException e) {
                throw new DatabaseException("Failed to fetch sessions for trainer.", e);
            }
            return list;
        }
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

    public boolean isTimeSlotBooked(int clientId, int trainerId,
                                    LocalDate sessionDate, LocalTime sessionTime) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(CHECK_DUPLICATE_TIME_SLOT)) {

            ps.setInt(1, clientId);
            ps.setInt(2, trainerId);
            ps.setDate(3, Date.valueOf(sessionDate));
            ps.setTime(4, Time.valueOf(sessionTime));

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to validate session time slot", e);
        }
    }


    private Session map(ResultSet rs) throws SQLException {
        Session session = new Session(
                rs.getInt("session_id"),
                rs.getInt("client_id"),
                rs.getInt("trainer_id"),
                rs.getDate("session_date").toLocalDate(),
                rs.getTime("session_time").toLocalTime(),
                SessionStatus.valueOf(rs.getString("status").toUpperCase()),
                rs.getString("notes")
        );
        session.setTrainerName(rs.getString("trainer_name"));
        return session;
    }
}
