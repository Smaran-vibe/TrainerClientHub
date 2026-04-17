package com.trainerclienthub.model;

import java.time.LocalDateTime;

public class Trainer {

    private int trainerId;
    private String name;
    private String email;
    private String phone;
    private String passwordHash;
    private TrainerRole role;
    private LocalDateTime createdAt;

    public Trainer() {
    }

    // Standard constructor for new trainer registrations
    public Trainer(String name, String email, String phone, String passwordHash) {
        setName(name);
        setEmail(email);
        setPhone(phone);
        setPasswordHash(passwordHash);
        this.role = TrainerRole.TRAINER; // Defaults to basic trainer role
        this.createdAt = LocalDateTime.now();
    }

    // Constructor used when specific roles (like ADMIN) need to be assigned
    public Trainer(String name, String email, String phone,
            String passwordHash, TrainerRole role) {
        setName(name);
        setEmail(email);
        setPhone(phone);
        setPasswordHash(passwordHash);
        setRole(role);
        this.createdAt = LocalDateTime.now();
    }

    // Full constructor for reconstructing trainer objects from database records
    public Trainer(int trainerId, String name, String email,
            String phone, String passwordHash,
            TrainerRole role, LocalDateTime createdAt) {
        this.trainerId = trainerId;
        setName(name);
        setEmail(email);
        setPhone(phone);
        setPasswordHash(passwordHash);
        setRole(role);
        this.createdAt = createdAt;
    }

    public int getTrainerId() {
        return trainerId;
    }

    public void setTrainerId(int trainerId) {
        this.trainerId = trainerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Trainer name must not be blank.");
        }
        this.name = name.trim();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        // Regex check for standard email structure
        if (email == null || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("Invalid email format: " + email);
        }
        this.email = email.trim().toLowerCase();
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Trainer phone must not be blank.");
        }
        String trimmed = phone.trim();

        // Specific validation for Nepal phone formats (+977, 977, or local 10-digit)
        if (!trimmed.matches("^(\\+977|977)?[0-9]{10}$")) {
            throw new IllegalArgumentException(
                    "Invalid Nepal phone number: \"" + trimmed + "\". "
                            + "Expected format: +977XXXXXXXXXX, 977XXXXXXXXXX, or XXXXXXXXXX "
                            + "(exactly 10 local digits after the optional +977 country code).");
        }
        this.phone = trimmed;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("Password hash must not be blank.");
        }
        this.passwordHash = passwordHash;
    }

    public TrainerRole getRole() {
        return role;
    }

    public void setRole(TrainerRole role) {
        if (role == null) {
            throw new IllegalArgumentException("Trainer role must not be null.");
        }
        this.role = role;
    }

    // Helper method for authorization checks in controllers
    public boolean isAdmin() {
        return role == TrainerRole.ADMIN;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Trainer{" +
                "trainerId=" + trainerId +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", role=" + role +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Trainer))
            return false;
        Trainer other = (Trainer) o;
        return trainerId == other.trainerId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(trainerId);
    }
}