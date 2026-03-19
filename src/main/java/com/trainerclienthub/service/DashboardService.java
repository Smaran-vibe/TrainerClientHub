package com.trainerclienthub.service;

import com.trainerclienthub.DAO.ClientDAO;
import com.trainerclienthub.DAO.MembershipDAO;
import com.trainerclienthub.DAO.PaymentDAO;
import com.trainerclienthub.DAO.SessionDAO;
import com.trainerclienthub.model.MembershipStatus;
import com.trainerclienthub.model.SessionStatus;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;

public class DashboardService {
    // Provides metrics and summaries for the dashboard.

    private final ClientDAO     clientDAO;
    private final MembershipDAO membershipDAO;
    private final SessionDAO    sessionDAO;
    private final PaymentDAO    paymentDAO;

    public DashboardService() {
        this.clientDAO     = new ClientDAO();
        this.membershipDAO = new MembershipDAO();
        this.sessionDAO    = new SessionDAO();
        this.paymentDAO    = new PaymentDAO();
    }

    public int getTotalClients() {
        return clientDAO.findAll().size();
    }

    public int getActiveMembershipCount() {
        return (int) membershipDAO.findAll().stream()
                .filter(m -> m.getStatus() == MembershipStatus.ACTIVE)
                .count();
    }

    public int getExpiringSoonCount(int days) {
        Date cutoff = Date.valueOf(LocalDate.now().plusDays(days));
        return membershipDAO.findExpiringBefore(cutoff).size();
    }

    public int getSessionsTodayCount() {
        return sessionDAO.findByDate(Date.valueOf(LocalDate.now())).size();
    }

    public int getSessionsCompletedTodayCount() {
        return (int) sessionDAO.findByDate(Date.valueOf(LocalDate.now()))
                .stream()
                .filter(s -> s.getStatus() == SessionStatus.COMPLETED)
                .count();
    }

    public BigDecimal getMonthlyRevenue() {
        YearMonth current = YearMonth.now();
        Date from = Date.valueOf(current.atDay(1));
        Date to   = Date.valueOf(current.atEndOfMonth());
        return paymentDAO.findByDateRange(from, to).stream()
                .map(p -> p.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getPreviousMonthRevenue() {
        YearMonth prev = YearMonth.now().minusMonths(1);
        Date from = Date.valueOf(prev.atDay(1));
        Date to   = Date.valueOf(prev.atEndOfMonth());
        return paymentDAO.findByDateRange(from, to).stream()
                .map(p -> p.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalRevenue() {
        return paymentDAO.findAll().stream()
                .map(p -> p.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public java.util.List<com.trainerclienthub.model.Payment> getRecentPayments(int limit) {
        return paymentDAO.findAll().stream()
                .limit(limit)
                .collect(java.util.stream.Collectors.toList());
    }

    public int getNewClientsThisMonth() {
        YearMonth current = YearMonth.now();
        return (int) clientDAO.findAll().stream()
                .filter(c -> c.getCreatedAt() != null &&
                        YearMonth.from(c.getCreatedAt()).equals(current))
                .count();
    }

    public int[] getMembershipGrowthData(int months) {
        int[] counts = new int[months];
        for (int i = 0; i < months; i++) {
            YearMonth ym = YearMonth.now().minusMonths(months - 1 - i);
            counts[i] = (int) clientDAO.findAll().stream()
                    .filter(c -> c.getCreatedAt() != null &&
                            YearMonth.from(c.getCreatedAt()).equals(ym))
                    .count();
        }
        return counts;
    }
}
