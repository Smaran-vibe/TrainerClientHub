package com.trainerclienthub.model;

/**
 * Lifecycle states for a {@link Payment} transaction.
 * Mirrors the ENUM constraint on the {@code payment} database table.
 */
public enum PaymentStatus {
    COMPLETED,
    PENDING,
    REFUNDED,
    FAILED
}
