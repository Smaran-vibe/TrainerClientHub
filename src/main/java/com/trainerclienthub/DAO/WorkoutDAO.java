package com.trainerclienthub.DAO;

import com.trainerclienthub.db.DatabaseConnection;
import com.trainerclienthub.db.DatabaseException;
import com.trainerclienthub.model.Workout;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class WorkoutDAO {

    private static final String INSERT =
            "INSERT INTO workout (client_id, trainer_id, workout_date, total_volume, notes) VALUES (?, ?, ?, ?, ?)";

private static final String WORKOUT_SELECT_BASE =
        "SELECT w.*, t.name AS trainer_name " +
        "FROM workout w " +
        "JOIN trainer t ON w.trainer_id = t.trainer_id ";

    private static final String SELECT_BY_ID =
            WORKOUT_SELECT_BASE + "WHERE w.workout_id = ?";

    private static final String SELECT_BY_CLIENT =
            WORKOUT_SELECT_BASE + "WHERE w.client_id = ? ORDER BY w.workout_date DESC";

    private static final String SELECT_BY_CLIENT_AND_DATE_RANGE =
            WORKOUT_SELECT_BASE + "WHERE w.client_id = ? AND w.workout_date BETWEEN ? AND ? ORDER BY w.workout_date DESC";

    private static final String SELECT_BY_TRAINER =
            WORKOUT_SELECT_BASE + "WHERE w.trainer_id = ? ORDER BY w.workout_date DESC";

    private static final String SELECT_ALL =
            WORKOUT_SELECT_BASE + "ORDER BY w.workout_date DESC";

    private static final String UPDATE =
            "UPDATE workout SET client_id = ?, trainer_id = ?, workout_date = ?, total_volume = ?, notes = ? " +
            "WHERE workout_id = ?";

    private static final String UPDATE_TOTAL_VOLUME =
            "UPDATE workout SET total_volume = ? WHERE workout_id = ?";

    private static final String DELETE =
            "DELETE FROM workout WHERE workout_id = ?";

    public void insert(Workout workout) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, workout.getClientId());
            ps.setInt(2, workout.getTrainerId());
            ps.setDate(3, Date.valueOf(workout.getWorkoutDate()));
            ps.setBigDecimal(4, workout.getTotalVolume());
            ps.setString(5, workout.getNotes());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) workout.setWorkoutId(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert workout for client id: " + workout.getClientId(), e);
        }
    }


    public Optional<Workout> findById(int workoutId) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_ID)) {

            ps.setInt(1, workoutId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find workout id: " + workoutId, e);
        }
    }

    public List<Workout> findByClient(int clientId) {
        List<Workout> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_CLIENT)) {

            ps.setInt(1, clientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch workouts for client id: " + clientId, e);
        }
        return list;
    }

    /** Returns workouts for a client within a date range (inclusive). */
    public List<Workout> findByClientAndDateRange(int clientId, Date from, Date to) {
        List<Workout> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_CLIENT_AND_DATE_RANGE)) {

            ps.setInt(1, clientId);
            ps.setDate(2, from);
            ps.setDate(3, to);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch workouts for client id " + clientId + " in date range.", e);
        }
        return list;
    }

    public List<Workout> findByTrainer(int trainerId) {
        List<Workout> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_TRAINER)) {

            ps.setInt(1, trainerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch workouts for trainer id: " + trainerId, e);
        }
        return list;
    }

    public List<Workout> findAll() {
        List<Workout> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch all workouts.", e);
        }
        return list;
    }


    public void update(Workout workout) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE)) {

            ps.setInt(1, workout.getClientId());
            ps.setInt(2, workout.getTrainerId());
            ps.setDate(3, Date.valueOf(workout.getWorkoutDate()));
            ps.setBigDecimal(4, workout.getTotalVolume());
            ps.setString(5, workout.getNotes());
            ps.setInt(6, workout.getWorkoutId());
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Failed to update workout id: " + workout.getWorkoutId(), e);
        }
    }

    /** Updates only the {@code total_volume} column after exercises are modified. */
    public void updateTotalVolume(int workoutId, java.math.BigDecimal totalVolume) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_TOTAL_VOLUME)) {

            ps.setBigDecimal(1, totalVolume);
            ps.setInt(2, workoutId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Failed to update total volume for workout id: " + workoutId, e);
        }
    }


    public void delete(int workoutId) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE)) {

            ps.setInt(1, workoutId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete workout id: " + workoutId, e);
        }
    }


    private Workout map(ResultSet rs) throws SQLException {
        Workout workout = new Workout(
                rs.getInt("workout_id"),
                rs.getInt("client_id"),
                rs.getInt("trainer_id"),
                rs.getDate("workout_date").toLocalDate(),
                rs.getBigDecimal("total_volume"),
                rs.getString("notes")
        );
        workout.setTrainerName(rs.getString("trainer_name"));
        return workout;
    }
}
