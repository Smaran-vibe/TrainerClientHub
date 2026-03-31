package com.trainerclienthub.service;

import com.trainerclienthub.DAO.MembershipDAO;
import com.trainerclienthub.DAO.ClientDAO;
import com.trainerclienthub.DAO.PaymentDAO;
import com.trainerclienthub.model.*;
import com.trainerclienthub.util.ValidationUtil;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Locale;

public class PaymentService {
    // Manages payment recording and queries.

    private MembershipDAO membershipDAO = new MembershipDAO();

    private ClientDAO clientDAO = new ClientDAO();

    private final PaymentDAO paymentDAO;

    public PaymentService() {
        this.paymentDAO = new PaymentDAO();
    }


    public Payment recordPayment(int clientId, int membershipId, BigDecimal amount,
                                 LocalDate paymentDate, PaymentMethod paymentMethod) {

        ValidationUtil.requirePositiveInt(clientId,     "Client ID");
        ValidationUtil.requirePositiveInt(membershipId, "Membership ID");
        ValidationUtil.requirePositiveDecimal(amount,   "Payment amount");
        ValidationUtil.requireNotFutureDate(paymentDate, "Payment date");

        if (paymentMethod == null) {
            throw new IllegalArgumentException("Payment method must not be null.");
        }

        Payment payment = new Payment(clientId, membershipId, amount,
                                      paymentDate, paymentMethod);
        paymentDAO.insert(payment);
        return payment;
    }


    public Optional<Payment> findById(int paymentId) {
        ValidationUtil.requirePositiveInt(paymentId, "Payment ID");
        return paymentDAO.findById(paymentId);
    }

    public List<Payment> findByClient(int clientId) {
        ValidationUtil.requirePositiveInt(clientId, "Client ID");
        return paymentDAO.findByClient(clientId);
    }

    public List<Payment> findByMembership(int membershipId) {
        ValidationUtil.requirePositiveInt(membershipId, "Membership ID");
        return paymentDAO.findByMembership(membershipId);
    }

    public List<Payment> findByDateRange(LocalDate from, LocalDate to) {
        ValidationUtil.requireEndAfterStart(from, to);
        return paymentDAO.findByDateRange(Date.valueOf(from), Date.valueOf(to));
    }

    public List<Payment> findAll() {
        return paymentDAO.findAll();
    }


    /**
     * Updates the payment status. Allowed values: COMPLETED, PENDING, REFUNDED, FAILED.
     */
    public void updatePaymentStatus(int paymentId, String newStatus) {
        ValidationUtil.requirePositiveInt(paymentId, "Payment ID");
        if (newStatus == null || newStatus.isBlank()) {
            throw new IllegalArgumentException("Payment status must not be null or blank.");
        }
        paymentDAO.updatePaymentStatus(paymentId, newStatus);

        handleMembershipStatusBasedOnPayment(paymentId, newStatus);
    }

    public void updatePaymentStatusAndMethod(int paymentId, String newStatus, PaymentMethod method) {
        ValidationUtil.requirePositiveInt(paymentId, "Payment ID");
        if (newStatus == null || newStatus.isBlank()) {
            throw new IllegalArgumentException("Payment status must not be null or blank.");
        }
        if (method == null) {
            throw new IllegalArgumentException("Payment method must not be null.");
        }
        paymentDAO.updatePaymentStatusAndMethod(paymentId, newStatus, method);

        handleMembershipStatusBasedOnPayment(paymentId, newStatus);
    }

    private void handleMembershipStatusBasedOnPayment(int paymentId, String newStatus) {
        if (newStatus == null || newStatus.isBlank()) {
            return;
        }

        String normalizedStatus = newStatus.strip().toUpperCase(Locale.ROOT);
        Optional<Payment> paymentOpt = paymentDAO.findById(paymentId);
        if (paymentOpt.isEmpty()) {
            return;
        }

        Payment payment = paymentOpt.get();
        Optional<Membership> membershipOpt = membershipDAO.findById(payment.getMembershipId());
        if (membershipOpt.isEmpty()) {
            return;
        }

        Membership membership = membershipOpt.get();

        boolean isCancellationStatus = normalizedStatus.equals("FAILED")
                || normalizedStatus.equals("REFUNDED")
                || normalizedStatus.equals("CANCELLED");

        if (isCancellationStatus) {
            membershipDAO.updateStatus(membership.getMembershipId(), MembershipStatus.CANCELLED);
            membershipDAO.findPlanById(membership.getPlanId())
                    .ifPresent(plan -> clientDAO.deductSessions(payment.getClientId(), plan.getSessionsIncluded()));
        } else if (normalizedStatus.equals("COMPLETED")) {
            membershipDAO.updateStatus(membership.getMembershipId(), MembershipStatus.ACTIVE);
        }
    }

    public void refundPayment(int paymentId) {
        ValidationUtil.requirePositiveInt(paymentId, "Payment ID");

        Payment payment = paymentDAO.findById(paymentId)
                .orElseThrow(() -> new IllegalStateException(
                        "Payment not found with ID: " + paymentId));

        if (payment.getPaymentStatus() == PaymentStatus.REFUNDED) {
            throw new IllegalStateException("Payment ID " + paymentId + " is already refunded.");
        }
        if (payment.getPaymentStatus() == PaymentStatus.FAILED) {
            throw new IllegalStateException(
                    "Failed payments cannot be refunded. Payment ID: " + paymentId);
        }

        paymentDAO.updateStatus(paymentId, PaymentStatus.REFUNDED);

        handleMembershipStatusBasedOnPayment(paymentId, "REFUNDED");
    }




    public void deletePayment(int paymentId) {
        ValidationUtil.requirePositiveInt(paymentId, "Payment ID");
        paymentDAO.delete(paymentId);
    }
}
