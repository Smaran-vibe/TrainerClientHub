package com.trainerclienthub.util;

import com.trainerclienthub.model.Trainer;
import com.trainerclienthub.model.TrainerRole;


public class SessionManager {

    private static SessionManager instance;
    private Trainer currentTrainer;

    private SessionManager() {
    }

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }


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


    public boolean isAdmin() {
        return currentTrainer != null && currentTrainer.isAdmin();
    }


    public TrainerRole getRole() {
        return currentTrainer != null ? currentTrainer.getRole() : TrainerRole.TRAINER;
    }
}