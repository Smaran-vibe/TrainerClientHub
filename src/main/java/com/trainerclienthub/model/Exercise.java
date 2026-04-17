package com.trainerclienthub.model;

import java.math.BigDecimal;

public class Exercise {
   private int exerciseId;
    private int workoutId;
    private String exerciseName;
    private int sets;
    private int reps;
    private BigDecimal weightKg;
    private BigDecimal volume;

    public Exercise() {
    }

    // For creating new exercises
    public Exercise(int workoutId, String exerciseName,
            int sets, int reps, BigDecimal weightKg) {
        setWorkoutId(workoutId);
        setExerciseName(exerciseName);
        setSets(sets);
        setReps(reps);
        setWeightKg(weightKg);
    }

    // For loading existing exercises with fixed ID
    public Exercise(int exerciseId, int workoutId, String exerciseName,
            int sets, int reps, BigDecimal weightKg) {
        this.exerciseId = exerciseId;
        setWorkoutId(workoutId);
        setExerciseName(exerciseName);
        setSets(sets);
        setReps(reps);
        setWeightKg(weightKg);
    }

    // Automatically called whenever sets, reps, or weight changes
    private void recalculateVolume() {
        if (weightKg != null && sets > 0 && reps > 0) {
            this.volume = weightKg
                    .multiply(BigDecimal.valueOf(sets))
                    .multiply(BigDecimal.valueOf(reps));
        } else {
            this.volume = BigDecimal.ZERO;
        }
    }

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

    public void setWeightKg(BigDecimal weightKg) {
        if (weightKg == null || weightKg.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Exercise weight must be greater than 0 kg. "
                            + "For bodyweight exercises use a minimum value of 1 kg.");
        }
        // Safety cap for data entry errors
        if (weightKg.compareTo(new BigDecimal("500.00")) > 0) {
            throw new IllegalArgumentException(
                    "Exercise weight cannot exceed 500 kg. Provided: " + weightKg);
        }
        this.weightKg = weightKg;
        recalculateVolume();
    }

    public BigDecimal getVolume() {
        return (volume != null) ? volume : BigDecimal.ZERO;
    }

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
        if (this == o)
            return true;
        if (!(o instanceof Exercise))
            return false;
        Exercise other = (Exercise) o;
        return exerciseId == other.exerciseId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(exerciseId);
    }
}