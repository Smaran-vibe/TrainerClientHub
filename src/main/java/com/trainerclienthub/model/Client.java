package com.trainerclienthub.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a gym member (client) in the Trainer-Client Hub system.
 * Maps to the {@code client} database table.
 *
 * <p>A Client is associated with exactly one {@link Trainer} who manages their
 * workouts and sessions. The {@code sessionBalance} tracks how many prepaid
 * training sessions the client has remaining.</p>
 */
public class Client {

    // ── Constants ────────────────────────────────────────────────────────────

    private static final int MIN_AGE = 10;
    private static final int MAX_AGE = 100;
    private static final BigDecimal MIN_WEIGHT = new BigDecimal("20.00");
    private static final BigDecimal MAX_WEIGHT = new BigDecimal("300.00");

    // ── Fields ──────────────────────────────────────────────────────────────

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

    // ── Constructors ─────────────────────────────────────────────────────────

    /** Default constructor required by the DAO layer when mapping ResultSets. */
    public Client() {}

    /**
     * Constructor used when registering a new client.
     *
     * @param name           client's full name
     * @param age            client's age (10–100)
     * @param gender         client's gender
     * @param phone          contact phone number
     * @param email          unique email address
     * @param weightKg       body weight in kilograms (20–300 kg)
     * @param sessionBalance initial number of prepaid sessions
     * @param trainerId      FK referencing the managing trainer
     */
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

    /**
     * Full constructor used when reconstructing a client from the database.
     *
     * @param clientId       database primary key
     * @param name           client's full name
     * @param age            client's age
     * @param gender         client's gender
     * @param phone          contact phone number
     * @param email          unique email address
     * @param sessionBalance remaining prepaid sessions
     * @param weightKg       body weight in kilograms
     * @param trainerId      FK referencing the managing trainer
     * @param createdAt      record creation timestamp
     */
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

    // ── Getters & Setters ────────────────────────────────────────────────────

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

    /**
     * Validates and sets the client's phone number.
     *
     * <p>Accepted formats (Nepal numbers only):
     * <ul>
     *   <li>{@code +9771111111111} — E.164 with country code, 10-digit local number</li>
     *   <li>{@code 9771111111111} — country code without plus, 10-digit local number</li>
     *   <li>{@code 1111111111}    — bare 10-digit local number</li>
     * </ul>
     * The local portion must be exactly 10 digits; no more, no less.</p>
     *
     * @param phone Nepal phone number in any accepted format
     * @throws IllegalArgumentException if the number is null, blank, or does not
     *                                  match a valid Nepal phone number pattern
     */
    public void setPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Client phone must not be blank.");
        }
        String trimmed = phone.trim();
        // Accepts: +977XXXXXXXXXX | 977XXXXXXXXXX | XXXXXXXXXX  (X = 10 local digits)
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

    /**
     * Convenience method to decrement the session balance by one.
     *
     * @throws IllegalStateException if session balance is already zero
     */
    public void decrementSessionBalance() {
        if (sessionBalance == 0) {
            throw new IllegalStateException("No remaining sessions to decrement for client: " + name);
        }
        this.sessionBalance--;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    /**
     * Validates and sets the client's body weight.
     *
     * @param weightKg weight in kilograms (must be between 20.00 and 300.00)
     * @throws IllegalArgumentException if weight is null or outside the valid range
     */
    public void setWeightKg(BigDecimal weightKg) {
        if (weightKg == null) {
            throw new IllegalArgumentException("Weight must not be null.");
        }
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

    // ── Object overrides ─────────────────────────────────────────────────────

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
