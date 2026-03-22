package com.trainerclienthub.service;

import com.trainerclienthub.DAO.ClientDAO;
import com.trainerclienthub.DAO.MembershipDAO;
import com.trainerclienthub.DAO.PaymentDAO;
import com.trainerclienthub.DAO.SessionDAO;
import com.trainerclienthub.DAO.WorkoutDAO;
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
    private final WorkoutDAO    workoutDAO;

    public DashboardService() {
        this.clientDAO     = new ClientDAO();
        this.membershipDAO = new MembershipDAO();
        this.sessionDAO    = new SessionDAO();
        this.paymentDAO    = new PaymentDAO();
        this.workoutDAO    = new WorkoutDAO();
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
        return paymentDAO.sumCompletedAmountByDateRange(from, to);
    }

    public BigDecimal getPreviousMonthRevenue() {
        YearMonth prev = YearMonth.now().minusMonths(1);
        Date from = Date.valueOf(prev.atDay(1));
        Date to   = Date.valueOf(prev.atEndOfMonth());
        return paymentDAO.sumCompletedAmountByDateRange(from, to);
    }

    public BigDecimal getTotalRevenue() {
        return paymentDAO.sumCompletedAmount();
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

    public java.util.List<com.trainerclienthub.model.Session> getTodaySessionsForTrainer(int trainerId) {
        LocalDate today = LocalDate.now();
        return sessionDAO.findByTrainer(trainerId).stream()
                .filter(s -> s.getSessionDate().equals(today))
                .sorted(java.util.Comparator.comparing(com.trainerclienthub.model.Session::getSessionTime))
                .collect(java.util.stream.Collectors.toList());
    }

    public int getPendingSessionsCountForTrainer(int trainerId) {
        return (int) sessionDAO.findByTrainer(trainerId).stream()
                .filter(s -> s.getStatus() == SessionStatus.SCHEDULED)
                .filter(s -> !s.getSessionDate().isBefore(LocalDate.now()))
                .count();
    }

    public int getWorkoutsThisWeekForTrainer(int trainerId) {
        LocalDate weekStart = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        LocalDate weekEnd   = weekStart.plusDays(6);
        return (int) workoutDAO.findByTrainer(trainerId).stream()
                .filter(w -> w.getWorkoutDate() != null &&
                        !w.getWorkoutDate().isBefore(weekStart) &&
                        !w.getWorkoutDate().isAfter(weekEnd))
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
