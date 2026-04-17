package com.trainerclienthub.DAO;

import com.trainerclienthub.db.DatabaseConnection;
import com.trainerclienthub.db.DatabaseException;
import com.trainerclienthub.model.Exercise;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class ExerciseDAO {


    private static final String INSERT =
            "INSERT INTO exercise (workout_id, exercise_name, sets, reps, weight_kg) VALUES (?, ?, ?, ?, ?)";

    private static final String SELECT_BY_ID =
            "SELECT * FROM exercise WHERE exercise_id = ?";

    private static final String SELECT_BY_WORKOUT =
            "SELECT * FROM exercise WHERE workout_id = ? ORDER BY exercise_id";

    private static final String UPDATE =
            "UPDATE exercise SET exercise_name = ?, sets = ?, reps = ?, weight_kg = ? WHERE exercise_id = ?";

    private static final String DELETE =
            "DELETE FROM exercise WHERE exercise_id = ?";

    private static final String DELETE_BY_WORKOUT =
            "DELETE FROM exercise WHERE workout_id = ?";


    public void insert(Exercise exercise) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, exercise.getWorkoutId());
            ps.setString(2, exercise.getExerciseName());
            ps.setInt(3, exercise.getSets());
            ps.setInt(4, exercise.getReps());
            ps.setBigDecimal(5, exercise.getWeightKg());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) exercise.setExerciseId(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert exercise for workout id: " + exercise.getWorkoutId(), e);
        }
    }


    public void insertBatch(List<Exercise> exercises) {
        if (exercises == null || exercises.isEmpty()) return;

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {

            for (Exercise exercise : exercises) {
                ps.setInt(1, exercise.getWorkoutId());
                ps.setString(2, exercise.getExerciseName());
                ps.setInt(3, exercise.getSets());
                ps.setInt(4, exercise.getReps());
                ps.setBigDecimal(5, exercise.getWeightKg());
                ps.addBatch();
            }

            ps.executeBatch();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                int i = 0;
                while (keys.next() && i < exercises.size()) {
                    exercises.get(i++).setExerciseId(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to batch-insert exercises.", e);
        }
    }


    public Optional<Exercise> findById(int exerciseId) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_ID)) {

            ps.setInt(1, exerciseId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find exercise id: " + exerciseId, e);
        }
    }


    public List<Exercise> findByWorkout(int workoutId) {
        List<Exercise> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_WORKOUT)) {

            ps.setInt(1, workoutId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch exercises for workout id: " + workoutId, e);
        }
        return list;
    }


    public void update(Exercise exercise) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE)) {

            ps.setString(1, exercise.getExerciseName());
            ps.setInt(2, exercise.getSets());
            ps.setInt(3, exercise.getReps());
            ps.setBigDecimal(4, exercise.getWeightKg());
            ps.setInt(5, exercise.getExerciseId());
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Failed to update exercise id: " + exercise.getExerciseId(), e);
        }
    }


    public void delete(int exerciseId) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE)) {

            ps.setInt(1, exerciseId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete exercise id: " + exerciseId, e);
        }
    }


    public void deleteByWorkout(int workoutId) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_BY_WORKOUT)) {

            ps.setInt(1, workoutId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete exercises for workout id: " + workoutId, e);
        }
    }


    private Exercise map(ResultSet rs) throws SQLException {
        return new Exercise(
                rs.getInt("exercise_id"),
                rs.getInt("workout_id"),
                rs.getString("exercise_name"),
                rs.getInt("sets"),
                rs.getInt("reps"),
                rs.getBigDecimal("weight_kg")
        );
    }
}
