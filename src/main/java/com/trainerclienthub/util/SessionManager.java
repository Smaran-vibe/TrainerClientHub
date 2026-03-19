package com.trainerclienthub.util;

import com.trainerclienthub.model.Trainer;
import com.trainerclienthub.model.TrainerRole;

/**
 * Application-scoped singleton that holds the currently authenticated trainer.
 */
public class SessionManager {

    private static SessionManager instance;
    private Trainer currentTrainer;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    // Session lifecycle

    /** Stores the authenticated trainer for the session. */
    public void login(Trainer trainer) {
        this.currentTrainer = trainer;
    }

    public Trainer getCurrentTrainer() {
        return currentTrainer;
    }

    public boolean isLoggedIn() {
        return currentTrainer != null;
    }

    public void logout() {
        this.currentTrainer = null;
    }

    // Role helpers

    /**
     *
     */
    public boolean isAdmin() {
        return currentTrainer != null && currentTrainer.isAdmin();
    }

    /**
     * Returns the role of the currently logged-in trainer, or
     */
    public TrainerRole getRole() {
        return currentTrainer != null ? currentTrainer.getRole() : TrainerRole.TRAINER;
    }
}