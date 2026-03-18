package com.trainerclienthub.model;

import java.math.BigDecimal;

/**
 * Represents a reusable membership plan template in the Trainer-Client Hub system.
 * Maps to the {@code membership_plan} database table.
 *
 * <p>A MembershipPlan defines the structure of a subscription offering (e.g.
 * "Monthly Basic", "Annual Premium"). Individual client subscriptions are
 * represented by {@link Membership}, which references this class via FK.</p>
 *
 * <p>This separation follows 3NF: plan attributes live here once and are
 * referenced — never duplicated — across all memberships that use this plan.</p>
 */
public class MembershipPlan {

    // ── Fields ──────────────────────────────────────────────────────────────

    private int planId;
    private String planName;
    private PlanType planType;
    private int durationDays;
    private BigDecimal price;

    // ── Constructors ─────────────────────────────────────────────────────────

    /** Default constructor required by the DAO layer when mapping ResultSets. */
    public MembershipPlan() {}

    /**
     * Constructor used when creating a new membership plan.
     *
     * @param planName     unique human-readable plan name
     * @param planType     duration category (Monthly, Quarterly, etc.)
     * @param durationDays number of calendar days the plan covers (must be > 0)
     * @param price        subscription price (must be >= 0)
     */
    public MembershipPlan(String planName, PlanType planType,
                          int durationDays, BigDecimal price) {
        setPlanName(planName);
        setPlanType(planType);
        setDurationDays(durationDays);
        setPrice(price);
    }

    /**
     * Full constructor used when reconstructing a plan from the database.
     *
     * @param planId       database primary key
     * @param planName     unique human-readable plan name
     * @param planType     duration category
     * @param durationDays number of calendar days the plan covers
     * @param price        subscription price
     */
    public MembershipPlan(int planId, String planName, PlanType planType,
                          int durationDays, BigDecimal price) {
        this.planId = planId;
        setPlanName(planName);
        setPlanType(planType);
        setDurationDays(durationDays);
        setPrice(price);
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public int getPlanId() {
        return planId;
    }

    public void setPlanId(int planId) {
        this.planId = planId;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        if (planName == null || planName.isBlank()) {
            throw new IllegalArgumentException("Plan name must not be blank.");
        }
        this.planName = planName.trim();
    }

    public PlanType getPlanType() {
        return planType;
    }

    public void setPlanType(PlanType planType) {
        if (planType == null) {
            throw new IllegalArgumentException("Plan type must not be null.");
        }
        this.planType = planType;
    }

    public int getDurationDays() {
        return durationDays;
    }

    public void setDurationDays(int durationDays) {
        if (durationDays <= 0) {
            throw new IllegalArgumentException("Duration must be greater than 0. Provided: " + durationDays);
        }
        this.durationDays = durationDays;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price must be zero or greater.");
        }
        this.price = price;
    }

    // ── Object overrides ─────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "MembershipPlan{" +
                "planId=" + planId +
                ", planName='" + planName + '\'' +
                ", planType=" + planType +
                ", durationDays=" + durationDays +
                ", price=" + price +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MembershipPlan)) return false;
        MembershipPlan other = (MembershipPlan) o;
        return planId == other.planId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(planId);
    }
}
