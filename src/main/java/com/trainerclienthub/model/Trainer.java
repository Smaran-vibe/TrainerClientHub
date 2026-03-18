package com.trainerclienthub.model;

import java.time.LocalDateTime;

/**
 * Represents a gym trainer account in the Trainer-Client Hub system.
 * Maps to the {@code trainer} database table.
 *
 * <p>A Trainer is the primary actor who manages clients, records workouts,
 * conducts sessions, and generates reports. Encapsulation is enforced by
 * keeping all fields private and exposing access only through validated
 * getters and setters.</p>
 *
 * <p>The {@link TrainerRole} field controls which screens and operations
 * are accessible after login:
 * <ul>
 *   <li>{@link TrainerRole#ADMIN}   — unrestricted access to all views</li>
 *   <li>{@link TrainerRole#TRAINER} — restricted to client, workout, session views</li>
 * </ul>
 * </p>
 */
public class Trainer {

    // ── Fields ──────────────────────────────────────────────────────────────

    private int trainerId;
    private String name;
    private String email;
    private String phone;
    private String passwordHash;
    private TrainerRole role;
    private LocalDateTime createdAt;

    // ── Constructors ─────────────────────────────────────────────────────────

    /** Default constructor required by the DAO layer when mapping ResultSets. */
    public Trainer() {}

    /**
     * Full constructor used when creating a new trainer record.
     * Role defaults to {@link TrainerRole#TRAINER} for self-registered accounts.
     * Use {@link #Trainer(String, String, String, String, TrainerRole)} to set ADMIN.
     *
     * @param name         trainer's full name
     * @param email        unique email address
     * @param phone        contact phone number
     * @param passwordHash BCrypt-hashed password (never store plaintext)
     */
    public Trainer(String name, String email, String phone, String passwordHash) {
        setName(name);
        setEmail(email);
        setPhone(phone);
        setPasswordHash(passwordHash);
        this.role      = TrainerRole.TRAINER;   // safe default for self-registration
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Constructor with explicit role — used by an admin creating another account.
     *
     * @param name         trainer's full name
     * @param email        unique email address
     * @param phone        contact phone number
     * @param passwordHash BCrypt-hashed password
     * @param role         {@link TrainerRole#ADMIN} or {@link TrainerRole#TRAINER}
     */
    public Trainer(String name, String email, String phone,
                   String passwordHash, TrainerRole role) {
        setName(name);
        setEmail(email);
        setPhone(phone);
        setPasswordHash(passwordHash);
        setRole(role);
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Full constructor used when reconstructing a trainer from the database.

     */
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

    // ── Getters & Setters ────────────────────────────────────────────────────

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
        if (email == null || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("Invalid email format: " + email);
        }
        this.email = email.trim().toLowerCase();
    }

    public String getPhone() {
        return phone;
    }

    /**
     * Validates and sets the trainer's phone number.
     *
     * <p>Accepted formats (Nepal numbers only):
     * <ul>
     *   <li>{@code +9771111111111} — E.164 with country code, 10-digit local number</li>
     *   <li>{@code 9771111111111} — country code without plus, 10-digit local number</li>
     *   <li>{@code 1111111111}    — bare 10-digit local number</li>
     * </ul>
     * The local number must start with 9 (mobile) or 1 (landline Kathmandu),
     * 2–8 (other landlines), for a total of exactly 10 local digits after the
     * optional {@code +977} / {@code 977} prefix.</p>
     *
     * @param phone Nepal phone number in any accepted format
     * @throws IllegalArgumentException if the number is null, blank, or does not
     *                                  match a valid Nepal phone number pattern
     */
    public void setPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Trainer phone must not be blank.");
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

    /**
     * Convenience method — returns {@code true} if this trainer holds the
     * {@link TrainerRole#ADMIN} role.
     *
     * <p>Use this in controllers to guard admin-only actions:</p>
     * <pre>{@code
     * if (!SessionManager.getInstance().getCurrentTrainer().isAdmin()) {
     *     showAlert("Access denied — admin only.");
     *     return;
     * }
     * }</pre>
     *
     * @return {@code true} if role is ADMIN
     */
    public boolean isAdmin() {
        return role == TrainerRole.ADMIN;
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
        if (this == o) return true;
        if (!(o instanceof Trainer)) return false;
        Trainer other = (Trainer) o;
        return trainerId == other.trainerId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(trainerId);
    }
}