package com.trainerclienthub.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Payment {

    private int paymentId;
    private int clientId;
    private String clientName;
    private int membershipId;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;

    public Payment() {
    }

    // Use this constructor when initializing a new transaction
    public Payment(int clientId, int membershipId, BigDecimal amount,
            LocalDate paymentDate, PaymentMethod paymentMethod) {
        setClientId(clientId);
        setMembershipId(membershipId);
        setAmount(amount);
        setPaymentDate(paymentDate);
        setPaymentMethod(paymentMethod);
        this.paymentStatus = PaymentStatus.PENDING; // New payments start as pending until confirmed
    }

    // Full constructor for loading historical payment data from the DB
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

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName == null ? null : clientName.trim();
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
        // Financial check to ensure no negative or zero-sum payments
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

    @Override
    public String toString() {
        return "Payment{" +
                "paymentId=" + paymentId +
                ", clientId=" + clientId +
                ", clientName='" + clientName + '\'' +
                ", membershipId=" + membershipId +
                ", amount=" + amount +
                ", paymentDate=" + paymentDate +
                ", paymentMethod=" + paymentMethod +
                ", paymentStatus=" + paymentStatus +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Payment))
            return false;
        Payment other = (Payment) o;
        return paymentId == other.paymentId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(paymentId);
    }
}