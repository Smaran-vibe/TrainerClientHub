package com.trainerclienthub.model;

/**
 * Lifecycle states for a scheduled training {@link Session}.
 * Mirrors the ENUM constraint on the {@code session} database table.
 */
public enum SessionStatus {
    SCHEDULED,
    COMPLETED,
    CANCELLED,
    NO_SHOW
}
