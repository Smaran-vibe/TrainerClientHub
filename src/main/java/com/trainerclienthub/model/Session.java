package com.trainerclienthub.model;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Represents a scheduled training session (calendar event) in the system.
 * Maps to the {@code session} database table.
 *
 * <p>A Session is a <em>calendar booking</em> — it records when a client is
 * scheduled to train with a specific trainer. It is intentionally separate
 * from {@link Workout}, which records the actual performance data. A session
 * may be booked and then completed, cancelled, or result in a no-show,
 * regardless of whether a workout record exists.</p>
 *
 * <p>Completing a session should trigger a decrement of the client's
 * {@code sessionBalance}, which is handled by the service layer.</p>
 */
public class Session {

    // ── Fields ──────────────────────────────────────────────────────────────

    private int sessionId;
    private int clientId;
    private int trainerId;
    private LocalDate sessionDate;
    private LocalTime sessionTime;
    private SessionStatus status;
    private String notes;

    // ── Constructors ─────────────────────────────────────────────────────────

    /** Default constructor required by the DAO layer when mapping ResultSets. */
    public Session() {}

    /**
     * Constructor used when booking a new session.
     *
     * @param clientId    FK referencing the client being trained
     * @param trainerId   FK referencing the conducting trainer
     * @param sessionDate date of the session
     * @param sessionTime time of the session
     * @param notes       optional trainer notes
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
     *
     * @param sessionId   database primary key
     * @param clientId    FK referencing the client
     * @param trainerId   FK referencing the trainer
     * @param sessionDate date of the session
     * @param sessionTime time of the session
     * @param status      current lifecycle status
     * @param notes       optional trainer notes
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

    // ── Getters & Setters ────────────────────────────────────────────────────

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

    // ── Convenience methods ───────────────────────────────────────────────────

    /** Returns {@code true} if this session is still in SCHEDULED state. */
    public boolean isScheduled() {
        return status == SessionStatus.SCHEDULED;
    }

    /** Returns {@code true} if this session has been marked as COMPLETED. */
    public boolean isCompleted() {
        return status == SessionStatus.COMPLETED;
    }

    // ── Object overrides ─────────────────────────────────────────────────────

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
