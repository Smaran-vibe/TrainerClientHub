package com.trainerclienthub.model;

/**
 * Lifecycle states for a {@link Membership}.
 * Mirrors the ENUM constraint on the {@code membership} database table.
 */
public enum MembershipStatus {
    ACTIVE,
    EXPIRED,
    CANCELLED
}
