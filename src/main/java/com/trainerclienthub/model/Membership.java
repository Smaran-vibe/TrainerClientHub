package com.trainerclienthub.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a specific client's subscription to a membership plan.
 * Maps to the {@code membership} database table.
 *
 * <p>A Membership is the instance that binds a {@link Client} to a
 * {@link MembershipPlan} for a defined date range. Historical memberships
 * are retained (status = EXPIRED or CANCELLED) rather than deleted,
 * enabling full audit history and reporting.</p>
 */
public class Membership {

    // ── Fields ──────────────────────────────────────────────────────────────

    private int membershipId;
    private int clientId;
    private int planId;
    private LocalDate startDate;
    private LocalDate endDate;
    private MembershipStatus status;
    private LocalDateTime createdAt;

    // ── Constructors ─────────────────────────────────────────────────────────

    /** Default constructor required by the DAO layer when mapping ResultSets. */
    public Membership() {}

    /**
     * Constructor used when creating a new membership for a client.
     *
     * @param clientId  FK referencing the client holding this membership
     * @param planId    FK referencing the chosen membership plan
     * @param startDate date the membership becomes active
     * @param endDate   date the membership expires (must be after startDate)
     */
    public Membership(int clientId, int planId, LocalDate startDate, LocalDate endDate) {
        setClientId(clientId);
        setPlanId(planId);
        setStartDate(startDate);
        setEndDate(endDate);
        this.status = MembershipStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Full constructor used when reconstructing a membership from the database.
     *
     * @param membershipId database primary key
     * @param clientId     FK referencing the client
     * @param planId       FK referencing the membership plan
     * @param startDate    membership activation date
     * @param endDate      membership expiry date
     * @param status       current lifecycle status
     * @param createdAt    record creation timestamp
     */
    public Membership(int membershipId, int clientId, int planId, LocalDate startDate,
                      LocalDate endDate, MembershipStatus status, LocalDateTime createdAt) {
        this.membershipId = membershipId;
        setClientId(clientId);
        setPlanId(planId);
        setStartDate(startDate);
        setEndDate(endDate);
        setStatus(status);
        this.createdAt = createdAt;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public int getMembershipId() {
        return membershipId;
    }

    public void setMembershipId(int membershipId) {
        this.membershipId = membershipId;
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

    public int getPlanId() {
        return planId;
    }

    public void setPlanId(int planId) {
        if (planId <= 0) {
            throw new IllegalArgumentException("Plan ID must be a positive integer.");
        }
        this.planId = planId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        if (startDate == null) {
            throw new IllegalArgumentException("Start date must not be null.");
        }
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        if (endDate == null) {
            throw new IllegalArgumentException("End date must not be null.");
        }
        if (startDate != null && !endDate.isAfter(startDate)) {
            throw new IllegalArgumentException("End date must be after start date.");
        }
        this.endDate = endDate;
    }

    public MembershipStatus getStatus() {
        return status;
    }

    public void setStatus(MembershipStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Membership status must not be null.");
        }
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // ── Convenience methods ───────────────────────────────────────────────────

    /**
     * Returns {@code true} if this membership is currently active and has not
     * passed its end date.
     */
    public boolean isCurrentlyActive() {
        return status == MembershipStatus.ACTIVE
                && !LocalDate.now().isAfter(endDate);
    }

    /**
     * Returns {@code true} if the membership end date is in the past
     * regardless of the recorded status field.
     */
    public boolean isPastExpiry() {
        return LocalDate.now().isAfter(endDate);
    }

    // ── Object overrides ─────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "Membership{" +
                "membershipId=" + membershipId +
                ", clientId=" + clientId +
                ", planId=" + planId +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", status=" + status +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Membership)) return false;
        Membership other = (Membership) o;
        return membershipId == other.membershipId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(membershipId);
    }
}
