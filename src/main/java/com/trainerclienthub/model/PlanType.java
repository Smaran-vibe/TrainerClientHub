package com.trainerclienthub.model;

/**
 * Defines the available membership plan duration categories.
 * Mirrors the ENUM constraint on the {@code membership_plan} database table.
 */
public enum PlanType {
    MONTHLY,
    QUARTERLY,
    ANNUAL,
    CUSTOM
}
