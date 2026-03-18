package com.trainerclienthub.model;

/**
 * Defines the two access roles a trainer account can hold.
 *
 * <ul>
 *   <li>{@link #ADMIN}   — full access: memberships, reports, trainer management,
 *                          all CRUD operations.</li>
 *   <li>{@link #TRAINER} — restricted access: manage own clients, log workouts,
 *                          schedule sessions. Cannot access Reports or manage
 *                          other trainers' clients.</li>
 * </ul>
 *
 * Mirrors the {@code ENUM('ADMIN','TRAINER')} constraint in the
 * {@code trainer} database table.
 */
public enum TrainerRole {
    ADMIN,
    TRAINER
}
