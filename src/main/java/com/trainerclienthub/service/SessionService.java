package com.trainerclienthub.service;

import com.trainerclienthub.DAO.ClientDAO;
import com.trainerclienthub.DAO.SessionDAO;
import com.trainerclienthub.model.Client;
import com.trainerclienthub.model.Session;
import com.trainerclienthub.model.SessionStatus;
import com.trainerclienthub.util.ValidationUtil;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public class SessionService {


    private final SessionDAO sessionDAO;
    private final ClientDAO clientDAO;

    public SessionService() {
        this.sessionDAO = new SessionDAO();
        this.clientDAO = new ClientDAO();
    }


    public Session scheduleSession(int clientId, int trainerId,
                                   LocalDate sessionDate, LocalTime sessionTime,
                                   String notes) {

        ValidationUtil.requirePositiveInt(clientId, "Client ID");
        ValidationUtil.requirePositiveInt(trainerId, "Trainer ID");

        if (sessionDate == null) {
            throw new IllegalArgumentException("Session date must not be null.");
        }
        if (sessionDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Session date cannot be in the past. "
                            + "Use workout logging to record past sessions.");
        }
        if (sessionTime == null) {
            throw new IllegalArgumentException("Session time must not be null.");
        }

        Client client = clientDAO.findById(clientId)
                .orElseThrow(() -> new IllegalStateException(
                        "Client not found with ID: " + clientId));

        if (client.getSessionBalance() < 1) {
            throw new IllegalStateException(
                    "Client \"" + client.getName() + "\" has no remaining session balance. "
                            + "Please assign a new session package before booking.");
        }

        if (sessionDAO.isTimeSlotBooked(clientId, trainerId, sessionDate, sessionTime)) {
            throw new IllegalArgumentException(
                    String.format("This time slot (%s %s) is already booked for client %s and trainer #%d.",
                            sessionDate, sessionTime, client.getName(), trainerId));
        }

        Session session = new Session(clientId, trainerId, sessionDate, sessionTime, notes);
        sessionDAO.insert(session);
        return session;
    }


    public Optional<Session> findById(int sessionId) {
        ValidationUtil.requirePositiveInt(sessionId, "Session ID");
        return sessionDAO.findById(sessionId);
    }

    public List<Session> findByClient(int clientId) {
        ValidationUtil.requirePositiveInt(clientId, "Client ID");
        return sessionDAO.findByClient(clientId);
    }

    public List<Session> findByTrainer(int trainerId) {
        ValidationUtil.requirePositiveInt(trainerId, "Trainer ID");
        return sessionDAO.findByTrainer(trainerId);
    }

    public List<Session> findByDate(LocalDate date) {
        if (date == null) throw new IllegalArgumentException("Date must not be null.");
        return sessionDAO.findByDate(Date.valueOf(date));
    }

    public List<Session> findUpcomingByClient(int clientId) {
        ValidationUtil.requirePositiveInt(clientId, "Client ID");
        return sessionDAO.findUpcomingByClient(clientId);
    }

    public List<Session> findAll() {
        return sessionDAO.findAll();
    }


    public void completeSession(int sessionId) {
        ValidationUtil.requirePositiveInt(sessionId, "Session ID");

        Session session = sessionDAO.findById(sessionId)
                .orElseThrow(() -> new IllegalStateException(
                        "Session not found with ID: " + sessionId));

        if (session.getStatus() != SessionStatus.SCHEDULED) {
            throw new IllegalStateException(
                    "Only SCHEDULED sessions can be completed. "
                            + "Current status: " + session.getStatus());
        }

        Client client = clientDAO.findById(session.getClientId())
                .orElseThrow(() -> new IllegalStateException(
                        "Client not found with ID: " + session.getClientId()));

        ValidationUtil.requireNonNegativeInt(client.getSessionBalance() - 1, "Session balance");

        int newBalance = client.getSessionBalance() - 1;
        clientDAO.updateSessionBalance(client.getClientId(), newBalance);
        sessionDAO.updateStatus(sessionId, SessionStatus.COMPLETED);
    }


    public void cancelSession(int sessionId) {
        ValidationUtil.requirePositiveInt(sessionId, "Session ID");

        Session session = sessionDAO.findById(sessionId)
                .orElseThrow(() -> new IllegalStateException(
                        "Session not found with ID: " + sessionId));

        if (session.getStatus() != SessionStatus.SCHEDULED) {
            throw new IllegalStateException(
                    "Only SCHEDULED sessions can be cancelled. "
                            + "Current status: " + session.getStatus());
        }

        sessionDAO.updateStatus(sessionId, SessionStatus.CANCELLED);
    }

    public void markNoShow(int sessionId) {
        ValidationUtil.requirePositiveInt(sessionId, "Session ID");

        Session session = sessionDAO.findById(sessionId)
                .orElseThrow(() -> new IllegalStateException(
                        "Session not found with ID: " + sessionId));

        if (session.getStatus() != SessionStatus.SCHEDULED) {
            throw new IllegalStateException(
                    "Only SCHEDULED sessions can be marked as no-show. "
                            + "Current status: " + session.getStatus());
        }

        Client client = clientDAO.findById(session.getClientId())
                .orElseThrow(() -> new IllegalStateException(
                        "Client not found with ID: " + session.getClientId()));

        int newBalance = Math.max(0, client.getSessionBalance() - 1);
        clientDAO.updateSessionBalance(client.getClientId(), newBalance);
        sessionDAO.updateStatus(sessionId, SessionStatus.NO_SHOW);
    }


    public void deleteSession(int sessionId) {
        ValidationUtil.requirePositiveInt(sessionId, "Session ID");
        sessionDAO.delete(sessionId);
    }
}
