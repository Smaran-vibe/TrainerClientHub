package com.trainerclienthub.service;

import com.trainerclienthub.DAO.ExerciseDAO;
import com.trainerclienthub.DAO.WorkoutDAO;
import com.trainerclienthub.model.Exercise;
import com.trainerclienthub.model.Workout;
import com.trainerclienthub.util.ValidationUtil;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class WorkoutService {
    // Coordinates workout and exercise persistence.

    private final WorkoutDAO  workoutDAO;
    private final ExerciseDAO exerciseDAO;

    public WorkoutService() {
        this.workoutDAO  = new WorkoutDAO();
        this.exerciseDAO = new ExerciseDAO();
    }

    public Workout logWorkout(int clientId, int trainerId, LocalDate workoutDate,
                              String notes, List<Exercise> exercises) {

        ValidationUtil.requirePositiveInt(clientId,  "Client ID");
        ValidationUtil.requirePositiveInt(trainerId, "Trainer ID");
        ValidationUtil.requireNotFutureDate(workoutDate, "Workout date");

        if (exercises == null || exercises.isEmpty()) {
            throw new IllegalArgumentException(
                    "A workout must contain at least one exercise.");
        }
        for (int i = 0; i < exercises.size(); i++) {
            validateExercise(exercises.get(i), i + 1);
        }

        Workout workout = new Workout(clientId, trainerId, workoutDate, notes);
        workoutDAO.insert(workout);

        exercises.forEach(e -> e.setWorkoutId(workout.getWorkoutId()));
        exerciseDAO.insertBatch(exercises);

        BigDecimal total = exercises.stream()
                .map(Exercise::getVolume)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        workout.setTotalVolume(total);
        workoutDAO.updateTotalVolume(workout.getWorkoutId(), total);

        workout.setExercises(exercises);
        return workout;
    }


    public Optional<Workout> findById(int workoutId) {
        ValidationUtil.requirePositiveInt(workoutId, "Workout ID");
        Optional<Workout> opt = workoutDAO.findById(workoutId);
        opt.ifPresent(w -> w.setExercises(exerciseDAO.findByWorkout(w.getWorkoutId())));
        return opt;
    }

    public List<Workout> findByClient(int clientId) {
        ValidationUtil.requirePositiveInt(clientId, "Client ID");
        List<Workout> workouts = workoutDAO.findByClient(clientId);
        workouts.forEach(w -> w.setExercises(exerciseDAO.findByWorkout(w.getWorkoutId())));
        return workouts;
    }

    public List<Workout> findByClientAndDateRange(int clientId,
                                                  LocalDate from, LocalDate to) {
        ValidationUtil.requirePositiveInt(clientId, "Client ID");
        ValidationUtil.requireEndAfterStart(from, to);
        List<Workout> workouts = workoutDAO.findByClientAndDateRange(
                clientId, Date.valueOf(from), Date.valueOf(to));
        workouts.forEach(w -> w.setExercises(exerciseDAO.findByWorkout(w.getWorkoutId())));
        return workouts;
    }


    public void updateExercise(Exercise exercise) {
        ValidationUtil.requirePositiveInt(exercise.getExerciseId(), "Exercise ID");
        validateExercise(exercise, 0);

        exerciseDAO.update(exercise);

        List<Exercise> siblings = exerciseDAO.findByWorkout(exercise.getWorkoutId());
        BigDecimal newTotal = siblings.stream()
                .map(Exercise::getVolume)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        workoutDAO.updateTotalVolume(exercise.getWorkoutId(), newTotal);
    }


    public void deleteWorkout(int workoutId) {
        ValidationUtil.requirePositiveInt(workoutId, "Workout ID");
        workoutDAO.delete(workoutId);   // DB CASCADE removes exercises automatically
    }


    private void validateExercise(Exercise e, int position) {
        String ctx = position > 0 ? "Exercise #" + position : "Exercise";

        ValidationUtil.requireNonBlank(e.getExerciseName(), ctx + " name");
        ValidationUtil.requirePositiveInt(e.getSets(),    ctx + " sets");
        ValidationUtil.requirePositiveInt(e.getReps(),    ctx + " reps");
        ValidationUtil.requirePositiveDecimal(e.getWeightKg(), ctx + " weight");

        if (e.getWeightKg().compareTo(new BigDecimal("500.00")) > 0) {
            throw new IllegalArgumentException(
                    ctx + " weight cannot exceed 500 kg. Provided: " + e.getWeightKg());
        }
    }
}
