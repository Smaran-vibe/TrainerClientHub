package com.trainerclienthub.model;

import java.math.BigDecimal;

public class MembershipPlan {

    private int planId;
    private String planName;
    private PlanType planType;
    private int durationDays;
    private BigDecimal price;
    private int sessionsIncluded;

    public MembershipPlan() {
    }

    // Constructor for defining new subscription levels
    public MembershipPlan(String planName, PlanType planType,
            int durationDays, BigDecimal price,
            int sessionsIncluded) {
        setPlanName(planName);
        setPlanType(planType);
        setDurationDays(durationDays);
        setPrice(price);
        setSessionsIncluded(sessionsIncluded);
    }

    // Constructor for loading existing plans from database
    public MembershipPlan(int planId, String planName, PlanType planType,
            int durationDays, BigDecimal price,
            int sessionsIncluded) {
        this.planId = planId;
        setPlanName(planName);
        setPlanType(planType);
        setDurationDays(durationDays);
        setPrice(price);
        setSessionsIncluded(sessionsIncluded);
    }

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
        // Enforce a minimum timeframe for any plan
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

    public int getSessionsIncluded() {
        return sessionsIncluded;
    }

    public void setSessionsIncluded(int sessionsIncluded) {
        // Validation for session-based or unlimited plans
        if (sessionsIncluded < 0) {
            throw new IllegalArgumentException("Sessions included cannot be negative.");
        }
        this.sessionsIncluded = sessionsIncluded;
    }

    @Override
    public String toString() {
        return "MembershipPlan{" +
                "planId=" + planId +
                ", planName='" + planName + '\'' +
                ", planType=" + planType +
                ", durationDays=" + durationDays +
                ", price=" + price +
                ", sessionsIncluded=" + sessionsIncluded +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof MembershipPlan))
            return false;
        MembershipPlan other = (MembershipPlan) o;
        return planId == other.planId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(planId);
    }
}