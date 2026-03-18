package com.trainerclienthub.model;

/**
 * Available payment methods for a {@link Payment} transaction.
 * Mirrors the ENUM constraint on the {@code payment} database table.
 */
public enum PaymentMethod {
    CASH,
    CARD,
    ONLINE,
    BANK_TRANSFER
}
