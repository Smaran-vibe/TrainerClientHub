package com.trainerclienthub.DAO;

import com.trainerclienthub.db.DatabaseConnection;
import com.trainerclienthub.db.DatabaseException;
import com.trainerclienthub.model.Client;
import com.trainerclienthub.model.Gender;
import com.trainerclienthub.model.TrainerRole;
import com.trainerclienthub.util.SessionManager;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class ClientDAO {


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

    private static final String INCREMENT_SESSION_BALANCE =
            "UPDATE client SET session_balance = session_balance + ? WHERE client_id = ?";

    private static final String DELETE =
            "DELETE FROM client WHERE client_id = ?";

    private static final String SEARCH =
            "SELECT * FROM client WHERE name LIKE ? OR email LIKE ? OR phone LIKE ? ORDER BY name";

    private static final String DEDUCT_SESSIONS =
            "UPDATE client SET session_balance = GREATEST(0, session_balance - ?) WHERE client_id = ?";

    private static final String PHONE_EXISTS =
            "SELECT 1 FROM client WHERE phone = ? LIMIT 1";


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

    public boolean phoneExists(String phone) {
        if (phone == null) {
            throw new IllegalArgumentException("Phone number must not be null.");
        }

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(PHONE_EXISTS)) {

            ps.setString(1, phone.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to check phone uniqueness for: " + phone, e);
        }
    }


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
        var role = SessionManager.getInstance().getRole();
        if (role == TrainerRole.TRAINER) {
            var trainer = SessionManager.getInstance().getCurrentTrainer();
            return trainer != null ? findByTrainer(trainer.getTrainerId()) : new ArrayList<>();
        }
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

    public void incrementSessionBalance(int clientId, int delta) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(INCREMENT_SESSION_BALANCE)) {

            ps.setInt(1, delta);
            ps.setInt(2, clientId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Failed to increment session balance for client id: " + clientId, e);
        }
    }


    public void delete(int clientId) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE)) {

            ps.setInt(1, clientId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete client id: " + clientId, e);
        }
    }

    public void deductSessions(int clientId, int sessionsToDeduct) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(DEDUCT_SESSIONS)) {

            ps.setInt(1, sessionsToDeduct);
            ps.setInt(2, clientId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Failed to deduct sessions for client id: " + clientId, e);
        }
    }

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
