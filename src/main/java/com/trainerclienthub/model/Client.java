package com.trainerclienthub.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;


public class Client {

    // Validation Constraints
    private static final int MIN_AGE = 10;
    private static final int MAX_AGE = 100;
    private static final BigDecimal MIN_WEIGHT = new BigDecimal("20.00");
    private static final BigDecimal MAX_WEIGHT = new BigDecimal("300.00");

    // Field
    private int clientId;
    private String name;
    private int age;
    private Gender gender;
    private String phone;
    private String email;
    private int sessionBalance;
    private BigDecimal weightKg;
    private int trainerId;
    private LocalDateTime createdAt;

    public Client() {}

    // Constructor for new clients (auto-sets timestamp)
    public Client(String name, int age, Gender gender, String phone,
                  String email, int sessionBalance, BigDecimal weightKg, int trainerId) {
        setName(name);
        setAge(age);
        setGender(gender);
        setPhone(phone);
        setEmail(email);
        setSessionBalance(sessionBalance);
        setWeightKg(weightKg);
        setTrainerId(trainerId);
        this.createdAt = LocalDateTime.now();
    }

    // Constructor for existing clients
    public Client(int clientId, String name, int age, Gender gender, String phone,
                  String email, int sessionBalance, BigDecimal weightKg, int trainerId, LocalDateTime createdAt) {
        this.clientId = clientId;
        setName(name);
        setAge(age);
        setGender(gender);
        setPhone(phone);
        setEmail(email);
        setSessionBalance(sessionBalance);
        setWeightKg(weightKg);
        setTrainerId(trainerId);
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public int getClientId() {
        return clientId;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Client name must not be blank.");
        }
        this.name = name.trim();
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        if (age < MIN_AGE || age > MAX_AGE) {
            throw new IllegalArgumentException(
                    "Age must be between " + MIN_AGE + " and " + MAX_AGE + ". Provided: " + age);
        }
        this.age = age;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        if (gender == null) {
            throw new IllegalArgumentException("Gender must not be null.");
        }
        this.gender = gender;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Client phone must not be blank.");
        }
        String trimmed = phone.trim();

        // Validates Nepal formats: +977..., 977..., or 10-digit local
        if (!trimmed.matches("^(\\+977|977)?[0-9]{10}$")) {
            throw new IllegalArgumentException(
                    "Invalid Nepal phone number: \"" + trimmed + "\". "
                            + "Expected format: +977XXXXXXXXXX, 977XXXXXXXXXX, or XXXXXXXXXX "
                            + "(exactly 10 local digits after the optional +977 country code).");
        }
        this.phone = trimmed;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        // Basic regex for standard email validation
        if (email == null || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("Invalid email format: " + email);
        }
        this.email = email.trim().toLowerCase();
    }

    public int getSessionBalance() {
        return sessionBalance;
    }

    public void setSessionBalance(int sessionBalance) {
        if (sessionBalance < 0) {
            throw new IllegalArgumentException("Session balance cannot be negative.");
        }
        this.sessionBalance = sessionBalance;
    }

    // Logic for burning a session after a workout
    public void decrementSessionBalance() {
        if (sessionBalance == 0) {
            throw new IllegalStateException("No remaining sessions to decrement for client: " + name);
        }
        this.sessionBalance--;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(BigDecimal weightKg) {
        if (weightKg == null) {
            throw new IllegalArgumentException("Weight must not be null.");
        }
        // Ensuring weight stays within limit
        if (weightKg.compareTo(MIN_WEIGHT) < 0 || weightKg.compareTo(MAX_WEIGHT) > 0) {
            throw new IllegalArgumentException(
                    "Weight must be between " + MIN_WEIGHT + " kg and " + MAX_WEIGHT + " kg. Provided: " + weightKg);
        }
        this.weightKg = weightKg;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Client{" +
                "clientId=" + clientId +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", weightKg=" + weightKg +
                ", sessionBalance=" + sessionBalance +
                ", trainerId=" + trainerId +
                '}';
    }

    // Clients are unique by their ID
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Client)) return false;
        Client other = (Client) o;
        return clientId == other.clientId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(clientId);
    }
}