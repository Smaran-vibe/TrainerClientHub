package com.trainerclienthub.model;

import java.math.BigDecimal;

public class Exercise {

    // ── Fields ──────────────────────────────────────────────────────────────

    private int exerciseId;
    private int workoutId;
    private String exerciseName;
    private int sets;
    private int reps;
    private BigDecimal weightKg;

    /** Derived: sets × reps × weightKg. Recalculated on any component change. */
    private BigDecimal volume;

    //  Constructors

    /** Default constructor required by the DAO layer when mapping ResultSets. */
    public Exercise() {}

    /**
     * Constructor used when adding a new exercise to a workout.
     *
     * @param workoutId    FK referencing the parent workout (must be > 0)
     * @param exerciseName name of the exercise (e.g. "Bench Press")
     * @param sets         number of sets performed (must be > 0)
     * @param reps         number of reps per set (must be > 0)
     * @param weightKg     weight used in kilograms (must be >= 0)
     */
    public Exercise(int workoutId, String exerciseName,
                    int sets, int reps, BigDecimal weightKg) {
        setWorkoutId(workoutId);
        setExerciseName(exerciseName);
        setSets(sets);
        setReps(reps);
        setWeightKg(weightKg);
    }

    /**
     * Full constructor used when reconstructing an exercise from the database.
     *
     * @param exerciseId   database primary key
     * @param workoutId    FK referencing the parent workout
     * @param exerciseName name of the exercise
     * @param sets         number of sets
     * @param reps         number of reps per set
     * @param weightKg     weight in kilograms
     */
    public Exercise(int exerciseId, int workoutId, String exerciseName,
                    int sets, int reps, BigDecimal weightKg) {
        this.exerciseId = exerciseId;
        setWorkoutId(workoutId);
        setExerciseName(exerciseName);
        setSets(sets);
        setReps(reps);
        setWeightKg(weightKg);
    }

    // ── Volume calculation ────────────────────────────────────────────────────

    /**
     * Recalculates volume as {@code sets × reps × weightKg}.
     * Called internally whenever any of the three components change.
     */
    private void recalculateVolume() {
        if (weightKg != null && sets > 0 && reps > 0) {
            this.volume = weightKg
                    .multiply(BigDecimal.valueOf(sets))
                    .multiply(BigDecimal.valueOf(reps));
        } else {
            this.volume = BigDecimal.ZERO;
        }
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public int getExerciseId() {
        return exerciseId;
    }

    public void setExerciseId(int exerciseId) {
        this.exerciseId = exerciseId;
    }

    public int getWorkoutId() {
        return workoutId;
    }

    public void setWorkoutId(int workoutId) {
        if (workoutId <= 0) {
            throw new IllegalArgumentException(
                    "Workout ID must be a positive integer. An Exercise cannot exist without a Workout.");
        }
        this.workoutId = workoutId;
    }

    public String getExerciseName() {
        return exerciseName;
    }

    public void setExerciseName(String exerciseName) {
        if (exerciseName == null || exerciseName.isBlank()) {
            throw new IllegalArgumentException("Exercise name must not be blank.");
        }
        this.exerciseName = exerciseName.trim();
    }

    public int getSets() {
        return sets;
    }

    public void setSets(int sets) {
        if (sets <= 0) {
            throw new IllegalArgumentException("Sets must be greater than 0. Provided: " + sets);
        }
        this.sets = sets;
        recalculateVolume();
    }

    public int getReps() {
        return reps;
    }

    public void setReps(int reps) {
        if (reps <= 0) {
            throw new IllegalArgumentException("Reps must be greater than 0. Provided: " + reps);
        }
        this.reps = reps;
        recalculateVolume();
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    /**
     * Sets the weight for this exercise.
     *
     * <p>Weight must be strictly greater than zero — bodyweight exercises
     * should use a symbolic minimum (e.g. 1 kg) rather than 0, because
     * a volume of 0 makes the exercise invisible in progress charts.</p>
     *
     * @param weightKg weight in kilograms (must be &gt; 0 and &le; 500)
     * @throws IllegalArgumentException if weight is null, zero, or negative
     */
    public void setWeightKg(BigDecimal weightKg) {
        if (weightKg == null || weightKg.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Exercise weight must be greater than 0 kg. "
                    + "For bodyweight exercises use a minimum value of 1 kg.");
        }
        if (weightKg.compareTo(new BigDecimal("500.00")) > 0) {
            throw new IllegalArgumentException(
                    "Exercise weight cannot exceed 500 kg. Provided: " + weightKg);
        }
        this.weightKg = weightKg;
        recalculateVolume();
    }

    /**
     * Returns the computed volume (sets × reps × weightKg).
     * This value is read-only from outside the class; it is always
     * derived from the three component fields.
     */
    public BigDecimal getVolume() {
        return (volume != null) ? volume : BigDecimal.ZERO;
    }

    // ── Object overrides ─────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "Exercise{" +
                "exerciseId=" + exerciseId +
                ", workoutId=" + workoutId +
                ", exerciseName='" + exerciseName + '\'' +
                ", sets=" + sets +
                ", reps=" + reps +
                ", weightKg=" + weightKg +
                ", volume=" + volume +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Exercise)) return false;
        Exercise other = (Exercise) o;
        return exerciseId == other.exerciseId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(exerciseId);
    }
}
