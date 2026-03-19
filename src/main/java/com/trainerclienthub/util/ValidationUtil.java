package com.trainerclienthub.util;

import java.math.BigDecimal;
import java.time.LocalDate;


public final class ValidationUtil {

    public static final String EMAIL_REGEX =
            "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";

    public static final String NEPAL_PHONE_REGEX =
            "^(\\+977|977)?[0-9]{10}$";

    //  Age constants

    public static final int MIN_AGE = 10;
    public static final int MAX_AGE = 100;

    //  Weight constants

    public static final BigDecimal MIN_CLIENT_WEIGHT = new BigDecimal("20.00");
    public static final BigDecimal MAX_CLIENT_WEIGHT = new BigDecimal("300.00");

    // Private constructor

    private ValidationUtil() {
        throw new UnsupportedOperationException("ValidationUtil is a utility class.");
    }


    //  Email

    public static boolean isValidEmail(String email) {
        return email != null && email.matches(EMAIL_REGEX);
    }

    /**
     * Ensures the email is valid, throwing an exception if not.
     *
     * @param email value to validate
     * @throws IllegalArgumentException if invalid
     */
    public static void requireValidEmail(String email) {
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException(
                    "Invalid email address. Please use the format: name@domain.com");
        }
    }

    public static boolean isValidNepalPhone(String phone) {
        return phone != null && phone.trim().matches(NEPAL_PHONE_REGEX);
    }


    public static void requireValidNepalPhone(String phone) {
        if (!isValidNepalPhone(phone)) {
            throw new IllegalArgumentException(
                    "Invalid Nepal phone number. "
                            + "Use +977XXXXXXXXXX, 977XXXXXXXXXX, or a 10-digit local number.");
        }
    }


    //  Age

    /**
     * Checks if age is within the allowed bounds.
     */
    public static boolean isValidAge(int age) {
        return age >= MIN_AGE && age <= MAX_AGE;
    }

    /**
     * Ensures age is within the allowed bounds.
     */
    public static void requireValidAge(int age) {
        if (!isValidAge(age)) {
            throw new IllegalArgumentException(
                    "Age must be between " + MIN_AGE + " and " + MAX_AGE
                            + ". Provided: " + age);
        }
    }


    //  Date of birth


    /**
     * Checks if the date of birth implies a valid age and is not in the future.
     */
    public static boolean isValidDateOfBirth(LocalDate dob) {
        if (dob == null) return false;
        LocalDate today = LocalDate.now();
        if (dob.isAfter(today)) return false;

        int impliedAge = today.getYear() - dob.getYear();
        if (today.getDayOfYear() < dob.getDayOfYear()) impliedAge--;

        return impliedAge >= MIN_AGE && impliedAge <= MAX_AGE;
    }

    /**
     * Ensures the date of birth implies a valid age and is not in the future.
     */
    public static void requireValidDateOfBirth(LocalDate dob) {
        if (dob == null) {
            throw new IllegalArgumentException("Date of birth must not be null.");
        }
        if (dob.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Date of birth cannot be in the future.");
        }
        if (!isValidDateOfBirth(dob)) {
            throw new IllegalArgumentException(
                    "Date of birth implies an age outside the allowed range ("
                            + MIN_AGE + "–" + MAX_AGE + " years).");
        }
    }


    //  Dates — general purpose


    /**
     * Checks if the date is today or in the past.
     */
    public static boolean isNotFuture(LocalDate date) {
        return date != null && !date.isAfter(LocalDate.now());
    }

    /**
     * Ensures the date is not in the future.
     */
    public static void requireNotFutureDate(LocalDate date, String label) {
        if (date == null) {
            throw new IllegalArgumentException(label + " must not be null.");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(label + " cannot be in the future.");
        }
    }

    /**
     * Ensures the end date strictly follows the start date.
     */
    public static void requireEndAfterStart(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            throw new IllegalArgumentException("Start date must not be null.");
        }
        if (endDate == null) {
            throw new IllegalArgumentException("End date must not be null.");
        }
        if (!endDate.isAfter(startDate)) {
            throw new IllegalArgumentException(
                    "End date (" + endDate + ") must be after start date (" + startDate + ").");
        }
    }


    //  Numeric positivity helpers


    /**
     * Ensures the integer is strictly greater than zero.
     */
    public static void requirePositiveInt(int value, String label) {
        if (value <= 0) {
            throw new IllegalArgumentException(
                    label + " must be greater than 0. Provided: " + value);
        }
    }

    /**
     * Ensures the BigDecimal is strictly greater than zero.
     */
    public static void requirePositiveDecimal(BigDecimal value, String label) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    label + " must be greater than zero. Provided: " + value);
        }
    }

    /**
     * Ensures the integer is zero or greater.
     */
    public static void requireNonNegativeInt(int value, String label) {
        if (value < 0) {
            throw new IllegalArgumentException(
                    label + " cannot be negative. Provided: " + value);
        }
    }

    //  String helpers


    /**
     * Ensures the string is not null or blank.
     */
    public static void requireNonBlank(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank.");
        }
    }

    //  Password


    /**
     * Ensures the password meets minimum length requirements.
     */
    public static void requireValidPassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException(
                    "Password must be at least 8 characters long.");
        }
    }

    /**
     * Ensures both passwords match.
     */
    public static void requirePasswordsMatch(String password, String confirmPassword) {
        if (password == null || !password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Passwords do not match.");
        }
    }


    //  Client weight


    /**
     * Ensures the client weight is within allowed bounds.
     */
    public static void requireValidClientWeight(BigDecimal weightKg) {
        if (weightKg == null) {
            throw new IllegalArgumentException("Weight must not be null.");
        }
        if (weightKg.compareTo(MIN_CLIENT_WEIGHT) < 0
                || weightKg.compareTo(MAX_CLIENT_WEIGHT) > 0) {
            throw new IllegalArgumentException(
                    "Weight must be between " + MIN_CLIENT_WEIGHT + " kg and "
                            + MAX_CLIENT_WEIGHT + " kg. Provided: " + weightKg);
        }
    }
}