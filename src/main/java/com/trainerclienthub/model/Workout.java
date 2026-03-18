package com.trainerclienthub.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a single workout session header in the Trainer-Client Hub system.
 * Maps to the {@code workout} database table.
 *
 * <p>A Workout is the parent record for one training session. It holds summary
 * data (date, total volume, notes) and owns a list of {@link Exercise} objects
 * that represent the individual exercises performed. This composition relationship
 * means Exercises cannot exist without their parent Workout.</p>
 *
 * <p>{@code totalVolume} is the aggregate of all child exercise volumes
 * (sets × reps × weight). It is kept in sync via {@link #recalculateTotalVolume()}
 * whenever the exercise list is modified.</p>
 */
public class Workout {

    // ── Fields ──────────────────────────────────────────────────────────────

    private int workoutId;
    private int clientId;
    private int trainerId;
    private LocalDate workoutDate;
    private BigDecimal totalVolume;
    private String notes;

    /** Child exercises — owned by this workout (composition). */
    private List<Exercise> exercises;

    // ── Constructors ─────────────────────────────────────────────────────────

    /** Default constructor required by the DAO layer when mapping ResultSets. */
    public Workout() {
        this.exercises   = new ArrayList<>();
        this.totalVolume = BigDecimal.ZERO;
    }

    /**
     * Constructor used when logging a new workout.
     *
     * @param clientId    FK referencing the client performing the workout
     * @param trainerId   FK referencing the supervising trainer
     * @param workoutDate date the workout took place
     * @param notes       optional trainer notes
     */
    public Workout(int clientId, int trainerId, LocalDate workoutDate, String notes) {
        setClientId(clientId);
        setTrainerId(trainerId);
        setWorkoutDate(workoutDate);
        this.notes       = notes;
        this.totalVolume = BigDecimal.ZERO;
        this.exercises   = new ArrayList<>();
    }

    /**
     * Full constructor used when reconstructing a workout from the database
     * (without child exercises — those are loaded separately by the DAO).
     *
     * @param workoutId   database primary key
     * @param clientId    FK referencing the client
     * @param trainerId   FK referencing the trainer
     * @param workoutDate date of the workout
     * @param totalVolume pre-computed total volume
     * @param notes       optional trainer notes
     */
    public Workout(int workoutId, int clientId, int trainerId,
                   LocalDate workoutDate, BigDecimal totalVolume, String notes) {
        this.workoutId  = workoutId;
        setClientId(clientId);
        setTrainerId(trainerId);
        setWorkoutDate(workoutDate);
        this.totalVolume = (totalVolume != null) ? totalVolume : BigDecimal.ZERO;
        this.notes       = notes;
        this.exercises   = new ArrayList<>();
    }

    // ── Exercise management ───────────────────────────────────────────────────

    /**
     * Adds an exercise to this workout and recalculates the total volume.
     *
     * @param exercise the exercise to add (must not be null)
     */
    public void addExercise(Exercise exercise) {
        if (exercise == null) {
            throw new IllegalArgumentException("Cannot add a null exercise to workout.");
        }
        exercises.add(exercise);
        recalculateTotalVolume();
    }

    /**
     * Removes an exercise from this workout and recalculates the total volume.
     *
     * @param exercise the exercise to remove
     */
    public void removeExercise(Exercise exercise) {
        exercises.remove(exercise);
        recalculateTotalVolume();
    }

    /**
     * Recalculates {@code totalVolume} as the sum of all child exercise volumes.
     * Should be called whenever the exercise list changes.
     */
    public void recalculateTotalVolume() {
        this.totalVolume = exercises.stream()
                .map(Exercise::getVolume)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public int getWorkoutId() {
        return workoutId;
    }

    public void setWorkoutId(int workoutId) {
        this.workoutId = workoutId;
    }

    public int getClientId() {
        return clientId;
    }

    public void setClientId(int clientId) {
        if (clientId <= 0) {
            throw new IllegalArgumentException("Client ID must be a positive integer.");
        }
        this.clientId = clientId;
    }

    public int getTrainerId() {
        return trainerId;
    }

    public void setTrainerId(int trainerId) {
        if (trainerId <= 0) {
            throw new IllegalArgumentException("Trainer ID must be a positive integer.");
        }
        this.trainerId = trainerId;
    }

    public LocalDate getWorkoutDate() {
        return workoutDate;
    }

    /**
     * Sets the date of this workout session.
     *
     * <p>The workout date must not be in the future — a workout can only be
     * recorded for a session that has already taken place.</p>
     *
     * @param workoutDate the date the workout occurred (must be today or earlier)
     * @throws IllegalArgumentException if the date is null or in the future
     */
    public void setWorkoutDate(LocalDate workoutDate) {
        if (workoutDate == null) {
            throw new IllegalArgumentException("Workout date must not be null.");
        }
        if (workoutDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Workout date cannot be in the future. "
                    + "A workout can only be logged after it has taken place.");
        }
        this.workoutDate = workoutDate;
    }

    public BigDecimal getTotalVolume() {
        return totalVolume;
    }

    public void setTotalVolume(BigDecimal totalVolume) {
        this.totalVolume = (totalVolume != null) ? totalVolume : BigDecimal.ZERO;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    /**
     * Returns an unmodifiable view of the exercise list.
     * Use {@link #addExercise(Exercise)} and {@link #removeExercise(Exercise)}
     * to modify the list so that total volume stays in sync.
     */
    public List<Exercise> getExercises() {
        return Collections.unmodifiableList(exercises);
    }

    public void setExercises(List<Exercise> exercises) {
        this.exercises = (exercises != null) ? new ArrayList<>(exercises) : new ArrayList<>();
        recalculateTotalVolume();
    }

    // ── Object overrides ─────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "Workout{" +
                "workoutId=" + workoutId +
                ", clientId=" + clientId +
                ", trainerId=" + trainerId +
                ", workoutDate=" + workoutDate +
                ", totalVolume=" + totalVolume +
                ", exerciseCount=" + exercises.size() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Workout)) return false;
        Workout other = (Workout) o;
        return workoutId == other.workoutId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(workoutId);
    }
}
