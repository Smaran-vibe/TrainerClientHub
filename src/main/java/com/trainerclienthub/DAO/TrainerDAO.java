package com.trainerclienthub.DAO;

import com.trainerclienthub.db.DatabaseConnection;
import com.trainerclienthub.db.DatabaseException;
import com.trainerclienthub.model.Trainer;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO for {@link Trainer} — handles all SQL operations on the {@code trainer} table.
 */
public class TrainerDAO {


    private static final String INSERT =
            "INSERT INTO trainer (name, email, phone, password_hash, role, created_at) VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SELECT_BY_ID =
            "SELECT * FROM trainer WHERE trainer_id = ?";

    private static final String SELECT_BY_EMAIL =
            "SELECT * FROM trainer WHERE email = ?";

    private static final String SELECT_BY_EMAIL_OR_PHONE =
            "SELECT * FROM trainer WHERE email = ? OR phone = ? LIMIT 1";

    private static final String SELECT_ALL =
            "SELECT * FROM trainer ORDER BY name";

    private static final String UPDATE =
            "UPDATE trainer SET name = ?, email = ?, phone = ?, password_hash = ?, role = ? WHERE trainer_id = ?";

    private static final String DELETE =
            "DELETE FROM trainer WHERE trainer_id = ?";

    private static final String SEARCH =
            "SELECT * FROM trainer WHERE name LIKE ? OR email LIKE ? ORDER BY name";


    /**
     * Inserts a new trainer and sets the generated {@code trainer_id} on the object
     */
    public void insert(Trainer trainer) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, trainer.getName());
            ps.setString(2, trainer.getEmail());
            ps.setString(3, trainer.getPhone());
            ps.setString(4, trainer.getPasswordHash());
            ps.setString(5, trainer.getRole() != null
                    ? trainer.getRole().name() : "TRAINER");
            ps.setTimestamp(6, Timestamp.valueOf(
                    trainer.getCreatedAt() != null ? trainer.getCreatedAt() : LocalDateTime.now()));

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    trainer.setTrainerId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert trainer: " + trainer.getEmail(), e);
        }
    }

    public Optional<Trainer> findById(int trainerId) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_ID)) {

            ps.setInt(1, trainerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find trainer by id: " + trainerId, e);
        }
    }

    public Optional<Trainer> findByEmail(String email) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_EMAIL)) {

            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find trainer by email: " + email, e);
        }
    }

    /**
     * Looks up a trainer by email address OR Nepal phone number.
     */
    public Optional<Trainer> findByEmailOrPhone(String emailOrPhone) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_EMAIL_OR_PHONE)) {

            ps.setString(1, emailOrPhone);
            ps.setString(2, emailOrPhone);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException(
                    "Failed to find trainer by email or phone: " + emailOrPhone, e);
        }
    }

    public List<Trainer> findAll() {
        List<Trainer> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch all trainers.", e);
        }
        return list;
    }

    /** Searches trainers by name or email (case-insensitive partial match). */
    public List<Trainer> search(String keyword) {
        List<Trainer> list = new ArrayList<>();
        String pattern = "%" + keyword + "%";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SEARCH)) {

            ps.setString(1, pattern);
            ps.setString(2, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to search trainers with keyword: " + keyword, e);
        }
        return list;
    }


    public void update(Trainer trainer) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE)) {

            ps.setString(1, trainer.getName());
            ps.setString(2, trainer.getEmail());
            ps.setString(3, trainer.getPhone());
            ps.setString(4, trainer.getPasswordHash());
            ps.setString(5, trainer.getRole() != null
                    ? trainer.getRole().name() : "TRAINER");
            ps.setInt(6, trainer.getTrainerId());
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Failed to update trainer id: " + trainer.getTrainerId(), e);
        }
    }


    public void delete(int trainerId) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE)) {

            ps.setInt(1, trainerId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete trainer id: " + trainerId, e);
        }
    }


    private Trainer map(ResultSet rs) throws SQLException {
        String roleStr = rs.getString("role");
        com.trainerclienthub.model.TrainerRole role =
                (roleStr != null && !roleStr.isBlank())
                        ? com.trainerclienthub.model.TrainerRole.valueOf(roleStr.toUpperCase())
                        : com.trainerclienthub.model.TrainerRole.TRAINER;

        return new Trainer(
                rs.getInt("trainer_id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getString("password_hash"),
                role,
                rs.getTimestamp("created_at").toLocalDateTime()
        );
    }
}
