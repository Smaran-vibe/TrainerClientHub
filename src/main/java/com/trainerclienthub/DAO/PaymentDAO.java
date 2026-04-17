package com.trainerclienthub.DAO;

import com.trainerclienthub.db.DatabaseConnection;
import com.trainerclienthub.db.DatabaseException;
import com.trainerclienthub.model.Payment;
import com.trainerclienthub.model.PaymentMethod;
import com.trainerclienthub.model.PaymentStatus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class PaymentDAO {


    private static final String INSERT =
            "INSERT INTO payment (client_id, membership_id, amount, payment_date, payment_method, payment_status) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SELECT_BY_ID =
            "SELECT * FROM payment WHERE payment_id = ?";

    private static final String SELECT_BY_CLIENT =
            "SELECT * FROM payment WHERE client_id = ? ORDER BY payment_date DESC";

    private static final String SELECT_BY_MEMBERSHIP =
            "SELECT * FROM payment WHERE membership_id = ? ORDER BY payment_date DESC";

    private static final String SELECT_BY_DATE_RANGE =
            "SELECT * FROM payment WHERE payment_date BETWEEN ? AND ? ORDER BY payment_date DESC";

    private static final String SELECT_ALL_WITH_CLIENT =
            "SELECT p.payment_id, p.client_id, p.membership_id, p.amount, " +
                    "p.payment_date, p.payment_method, p.payment_status, c.name AS client_name " +
                    "FROM payment p " +
                    "JOIN client c ON p.client_id = c.client_id " +
                    "ORDER BY p.payment_date DESC";

    private static final String UPDATE =
            "UPDATE payment SET client_id = ?, membership_id = ?, amount = ?, payment_date = ?, " +
                    "payment_method = ?, payment_status = ? WHERE payment_id = ?";

    private static final String UPDATE_STATUS =
            "UPDATE payment SET payment_status = ? WHERE payment_id = ?";

    private static final String UPDATE_STATUS_AND_METHOD =
            "UPDATE payment SET payment_status = ?, payment_method = ? WHERE payment_id = ?";

    private static final String SUM_COMPLETED_BY_DATE_RANGE =
            "SELECT COALESCE(SUM(amount), 0) FROM payment WHERE payment_status = 'COMPLETED' AND payment_date BETWEEN ? AND ?";

    private static final String SUM_COMPLETED_ALL =
            "SELECT COALESCE(SUM(amount), 0) FROM payment WHERE payment_status = 'COMPLETED'";

    private static final String DELETE =
            "DELETE FROM payment WHERE payment_id = ?";


    public void insert(Payment payment) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, payment.getClientId());
            ps.setInt(2, payment.getMembershipId());
            ps.setBigDecimal(3, payment.getAmount());
            ps.setDate(4, Date.valueOf(payment.getPaymentDate()));
            ps.setString(5, payment.getPaymentMethod().name());
            ps.setString(6, payment.getPaymentStatus().name());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) payment.setPaymentId(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert payment for client id: " + payment.getClientId(), e);
        }
    }


    public Optional<Payment> findById(int paymentId) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_ID)) {

            ps.setInt(1, paymentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find payment id: " + paymentId, e);
        }
    }

    public List<Payment> findByClient(int clientId) {
        List<Payment> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_CLIENT)) {

            ps.setInt(1, clientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch payments for client id: " + clientId, e);
        }
        return list;
    }

    public List<Payment> findByMembership(int membershipId) {
        List<Payment> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_MEMBERSHIP)) {

            ps.setInt(1, membershipId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch payments for membership id: " + membershipId, e);
        }
        return list;
    }

    public List<Payment> findByDateRange(Date from, Date to) {
        List<Payment> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_DATE_RANGE)) {

            ps.setDate(1, from);
            ps.setDate(2, to);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch payments in date range.", e);
        }
        return list;
    }

    public List<Payment> findAll() {
        List<Payment> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ALL_WITH_CLIENT);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) list.add(map(rs, true));
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch all payments.", e);
        }
        return list;
    }


    public void update(Payment payment) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE)) {

            ps.setInt(1, payment.getClientId());
            ps.setInt(2, payment.getMembershipId());
            ps.setBigDecimal(3, payment.getAmount());
            ps.setDate(4, Date.valueOf(payment.getPaymentDate()));
            ps.setString(5, payment.getPaymentMethod().name());
            ps.setString(6, payment.getPaymentStatus().name());
            ps.setInt(7, payment.getPaymentId());
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Failed to update payment id: " + payment.getPaymentId(), e);
        }
    }


    public void updateStatus(int paymentId, PaymentStatus status) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_STATUS)) {

            ps.setString(1, status.name());
            ps.setInt(2, paymentId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Failed to update status for payment id: " + paymentId, e);
        }
    }

    public void updateStatusAndMethod(int paymentId, PaymentStatus status, PaymentMethod method) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE_STATUS_AND_METHOD)) {

            ps.setString(1, status.name());
            ps.setString(2, method.name());
            ps.setInt(3, paymentId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Update Failed for ID " + paymentId + " | DB Error: " + e.getMessage(), e);
        }
    }

    public void updatePaymentStatusAndMethod(int paymentId, String newStatus, PaymentMethod method) {
        updateStatusAndMethod(paymentId, PaymentStatus.valueOf(newStatus.toUpperCase().trim()), method);
    }


    public void updatePaymentStatus(int paymentId, String newStatus) {
        updateStatus(paymentId, PaymentStatus.valueOf(newStatus.toUpperCase().trim()));
    }


    public java.math.BigDecimal sumCompletedAmountByDateRange(Date from, Date to) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SUM_COMPLETED_BY_DATE_RANGE)) {

            ps.setDate(1, from);
            ps.setDate(2, to);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return java.math.BigDecimal.ZERO;
                java.math.BigDecimal val = rs.getBigDecimal(1);
                return val != null ? val : java.math.BigDecimal.ZERO;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to sum completed revenue by date range.", e);
        }
    }


    public java.math.BigDecimal sumCompletedAmount() {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SUM_COMPLETED_ALL);
             ResultSet rs = ps.executeQuery()) {

            if (!rs.next()) return java.math.BigDecimal.ZERO;
            java.math.BigDecimal val = rs.getBigDecimal(1);
            return val != null ? val : java.math.BigDecimal.ZERO;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to sum completed revenue.", e);
        }
    }


    public void delete(int paymentId) {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE)) {

            ps.setInt(1, paymentId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete payment id: " + paymentId, e);
        }
    }


    private Payment map(ResultSet rs) throws SQLException {
        return map(rs, false);
    }

    private Payment map(ResultSet rs, boolean includeClientName) throws SQLException {
        Payment payment = new Payment(
                rs.getInt("payment_id"),
                rs.getInt("client_id"),
                rs.getInt("membership_id"),
                rs.getBigDecimal("amount"),
                rs.getDate("payment_date").toLocalDate(),
                PaymentMethod.valueOf(rs.getString("payment_method").toUpperCase()),
                PaymentStatus.valueOf(rs.getString("payment_status").toUpperCase())
        );
        if (includeClientName) {
            String clientName = rs.getString("client_name");
            payment.setClientName(clientName);
        }
        return payment;
    }


    public List<Payment> findRecent(int limit) {
        String sql = "SELECT * FROM payment ORDER BY payment_date DESC LIMIT ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<Payment> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to fetch recent payments.", e);
        }
    }
}
