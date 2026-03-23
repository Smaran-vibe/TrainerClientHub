package com.trainerclienthub.model;

import java.time.LocalDate;
import java.time.LocalTime;

public class Session {

    // ── Fields ──────────────────────────────────────────────────────────────

    private int sessionId;
    private int clientId;
    private int trainerId;
    private String trainerName;
    private LocalDate sessionDate;
    private LocalTime sessionTime;
    private SessionStatus status;
    private String notes;

    // ── Constructors ─────────────────────────────────────────────────────────

    /** Default constructor required by the DAO layer when mapping ResultSets. */
    public Session() {}

    /**
     * Constructor used when booking a new session.
     */
    public Session(int clientId, int trainerId,
                   LocalDate sessionDate, LocalTime sessionTime, String notes) {
        setClientId(clientId);
        setTrainerId(trainerId);
        setSessionDate(sessionDate);
        setSessionTime(sessionTime);
        this.status = SessionStatus.SCHEDULED;
        this.notes  = notes;
    }

    /**
     * Full constructor used when reconstructing a session from the database.
     */
    public Session(int sessionId, int clientId, int trainerId,
                   LocalDate sessionDate, LocalTime sessionTime,
                   SessionStatus status, String notes) {
        this.sessionId = sessionId;
        setClientId(clientId);
        setTrainerId(trainerId);
        setSessionDate(sessionDate);
        setSessionTime(sessionTime);
        setStatus(status);
        this.notes = notes;
    }

    //  Getters & Setters

    public int getSessionId() {
        return sessionId;
    }

    public void setSessionId(int sessionId) {
        this.sessionId = sessionId;
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

    public String getTrainerName() {
        return trainerName;
    }

    public void setTrainerName(String trainerName) {
        this.trainerName = trainerName == null ? null : trainerName.trim();
    }

    public LocalDate getSessionDate() {
        return sessionDate;
    }

    public void setSessionDate(LocalDate sessionDate) {
        if (sessionDate == null) {
            throw new IllegalArgumentException("Session date must not be null.");
        }
        this.sessionDate = sessionDate;
    }

    public LocalTime getSessionTime() {
        return sessionTime;
    }

    public void setSessionTime(LocalTime sessionTime) {
        if (sessionTime == null) {
            throw new IllegalArgumentException("Session time must not be null.");
        }
        this.sessionTime = sessionTime;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Session status must not be null.");
        }
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    //Convenience methods

    /** Returns {@code true} if this session is still in SCHEDULED state. */
    public boolean isScheduled() {
        return status == SessionStatus.SCHEDULED;
    }

    /** Returns {@code true} if this session has been marked as COMPLETED. */
    public boolean isCompleted() {
        return status == SessionStatus.COMPLETED;
    }

    //Object overrides

    @Override
    public String toString() {
        return "Session{" +
                "sessionId=" + sessionId +
                ", clientId=" + clientId +
                ", trainerId=" + trainerId +
                ", sessionDate=" + sessionDate +
                ", sessionTime=" + sessionTime +
                ", status=" + status +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Session)) return false;
        Session other = (Session) o;
        return sessionId == other.sessionId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(sessionId);
    }
}
