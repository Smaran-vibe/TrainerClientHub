package com.trainerclienthub.DAO;

import com.trainerclienthub.db.DatabaseConnection;
import com.trainerclienthub.db.DatabaseException;
import com.trainerclienthub.model.Client;
import com.trainerclienthub.model.Gender;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO for {@link Client} — handles all SQL operations on the {@code client} table.
 */
public class ClientDAO {

    // ── SQL ──────────────────────────────────────────────────────────────────

    private static final String INSERT =
            "INSERT INTO client (name, age, gender, phone, email, session_balance, weight_kg, trainer_id, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SELECT_BY_ID =
            "SELECT * FROM client WHERE client_id = ?";

    private static final String SELECT_BY_EMAIL =
            "SELECT * FROM client WHERE email = ?";

    private static final String SELECT_ALL =
            "SELECT * FROM client ORDER BY name";

    private static final String SELECT_BY_TRAINER =
            "SELECT * FROM client WHERE trainer_id = ? ORDER BY name";

    private static final String UPDATE =
            "UPDATE client SET name = ?, age = ?, gender = ?, phone = ?, email = ?, " +
                    "session_balance = ?, weight_kg = ?, trainer_id = ? WHERE client_id = ?";

    private static final String UPDATE_SESSION_BALANCE =
            "UPDATE client SET session_balance = ? WHERE client_id = ?";

    private static final String DELETE =
            "DELETE FROM client WHERE client_id = ?";

    private static final String SEARCH =
            "SELECT * FROM client WHERE name LIKE ? OR email LIKE ? OR phone LIKE ? ORDER BY name";

    // ── Create ───────────────────────────────────────────────────────────────

    public void insert(Client client) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, client.getName());
            ps.setInt(2, client.getAge());
            ps.setString(3, client.getGender().name());
            ps.setString(4, client.getPhone());
            ps.setString(5, client.getEmail());
            ps.setInt(6, client.getSessionBalance());
            ps.setBigDecimal(7, client.getWeightKg());
            ps.setInt(8, client.getTrainerId());
            ps.setTimestamp(9, Timestamp.valueOf(
                    client.getCreatedAt() != null ? client.getCreatedAt() : LocalDateTime.now()));

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) client.setClientId(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert client: " + client.getEmail(), e);
        }
    }

    // ── Read ─────────────────────────────────────────────────────────────────

    public Optional<Client> findById(int clientId) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_ID)) {

            ps.setInt(1, clientId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find client by id: " + clientId, e);
        }
    }

    public Optional<Client> findByEmail(String email) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_EMAIL)) {

            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find client by email: " + email, e);
        }
    }

    public List<Client> findAll() {
        List<Client> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch all clients.", e);
        }
        return list;
    }

    public List<Client> findByTrainer(int trainerId) {
        List<Client> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_TRAINER)) {

            ps.setInt(1, trainerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch clients for trainer id: " + trainerId, e);
        }
        return list;
    }

    /** Searches by name, email, or phone (partial match). */
    public List<Client> search(String keyword) {
        List<Client> list = new ArrayList<>();
        String pattern = "%" + keyword + "%";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SEARCH)) {

            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to search clients: " + keyword, e);
        }
        return list;
    }

    // ── Update ───────────────────────────────────────────────────────────────

    public void update(Client client) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE)) {

            ps.setString(1, client.getName());
            ps.setInt(2, client.getAge());
            ps.setString(3, client.getGender().name());
            ps.setString(4, client.getPhone());
            ps.setString(5, client.getEmail());
            ps.setInt(6, client.getSessionBalance());
            ps.setBigDecimal(7, client.getWeightKg());
            ps.setInt(8, client.getTrainerId());
            ps.setInt(9, client.getClientId());
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Failed to update client id: " + client.getClientId(), e);
        }
    }

    /** Updates only the session balance column — avoids a full-row update. */
    public void updateSessionBalance(int clientId, int newBalance) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_SESSION_BALANCE)) {

            ps.setInt(1, newBalance);
            ps.setInt(2, clientId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Failed to update session balance for client id: " + clientId, e);
        }
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    public void delete(int clientId) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE)) {

            ps.setInt(1, clientId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete client id: " + clientId, e);
        }
    }

    // ── Mapper ───────────────────────────────────────────────────────────────

    private Client map(ResultSet rs) throws SQLException {
        return new Client(
                rs.getInt("client_id"),
                rs.getString("name"),
                rs.getInt("age"),
                Gender.valueOf(rs.getString("gender").toUpperCase()),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getInt("session_balance"),
                rs.getBigDecimal("weight_kg"),
                rs.getInt("trainer_id"),
                rs.getTimestamp("created_at").toLocalDateTime()
        );
    }
}