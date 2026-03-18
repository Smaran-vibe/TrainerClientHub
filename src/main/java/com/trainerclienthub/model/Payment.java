package com.trainerclienthub.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents a financial transaction linked to a client's membership.
 * Maps to the {@code payment} database table.
 *
 * <p>A Payment records a single monetary transaction that a client makes
 * in relation to a specific {@link Membership}. Both the {@code clientId}
 * and {@code membershipId} FKs are stored to allow querying by either
 * dimension independently (e.g. all payments by a client, or all payments
 * for a specific membership renewal).</p>
 */
public class Payment {

    // ── Fields ──────────────────────────────────────────────────────────────

    private int paymentId;
    private int clientId;
    private int membershipId;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;

    // ── Constructors ─────────────────────────────────────────────────────────

    /** Default constructor required by the DAO layer when mapping ResultSets. */
    public Payment() {}

    /**
     * Constructor used when recording a new payment.
     *
     * @param clientId      FK referencing the paying client
     * @param membershipId  FK referencing the membership being paid for
     * @param amount        transaction amount (must be > 0)
     * @param paymentDate   date the payment was made
     * @param paymentMethod how the payment was made
     */
    public Payment(int clientId, int membershipId, BigDecimal amount,
                   LocalDate paymentDate, PaymentMethod paymentMethod) {
        setClientId(clientId);
        setMembershipId(membershipId);
        setAmount(amount);
        setPaymentDate(paymentDate);
        setPaymentMethod(paymentMethod);
        this.paymentStatus = PaymentStatus.COMPLETED;
    }

    /**
     * Full constructor used when reconstructing a payment from the database.
     *
     * @param paymentId     database primary key
     * @param clientId      FK referencing the client
     * @param membershipId  FK referencing the membership
     * @param amount        transaction amount
     * @param paymentDate   date of the transaction
     * @param paymentMethod payment method used
     * @param paymentStatus current status of the transaction
     */
    public Payment(int paymentId, int clientId, int membershipId, BigDecimal amount,
                   LocalDate paymentDate, PaymentMethod paymentMethod, PaymentStatus paymentStatus) {
        this.paymentId = paymentId;
        setClientId(clientId);
        setMembershipId(membershipId);
        setAmount(amount);
        setPaymentDate(paymentDate);
        setPaymentMethod(paymentMethod);
        setPaymentStatus(paymentStatus);
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public int getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(int paymentId) {
        this.paymentId = paymentId;
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

    public int getMembershipId() {
        return membershipId;
    }

    public void setMembershipId(int membershipId) {
        if (membershipId <= 0) {
            throw new IllegalArgumentException("Membership ID must be a positive integer.");
        }
        this.membershipId = membershipId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero.");
        }
        this.amount = amount;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        if (paymentDate == null) {
            throw new IllegalArgumentException("Payment date must not be null.");
        }
        this.paymentDate = paymentDate;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        if (paymentMethod == null) {
            throw new IllegalArgumentException("Payment method must not be null.");
        }
        this.paymentMethod = paymentMethod;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        if (paymentStatus == null) {
            throw new IllegalArgumentException("Payment status must not be null.");
        }
        this.paymentStatus = paymentStatus;
    }

    // ── Object overrides ─────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "Payment{" +
                "paymentId=" + paymentId +
                ", clientId=" + clientId +
                ", membershipId=" + membershipId +
                ", amount=" + amount +
                ", paymentDate=" + paymentDate +
                ", paymentMethod=" + paymentMethod +
                ", paymentStatus=" + paymentStatus +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Payment)) return false;
        Payment other = (Payment) o;
        return paymentId == other.paymentId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(paymentId);
    }
}
