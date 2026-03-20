package com.trainerclienthub.service;

import com.trainerclienthub.DAO.PaymentDAO;
import com.trainerclienthub.model.Payment;
import com.trainerclienthub.model.PaymentMethod;
import com.trainerclienthub.model.PaymentStatus;
import com.trainerclienthub.util.ValidationUtil;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class PaymentService {
    // Manages payment recording and queries.

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
    }


    public void deletePayment(int paymentId) {
        ValidationUtil.requirePositiveInt(paymentId, "Payment ID");
        paymentDAO.delete(paymentId);
    }
}
