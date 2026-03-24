package com.trainerclienthub.service;
import com.trainerclienthub.DAO.ClientDAO;
import com.trainerclienthub.DAO.ExerciseDAO;
import com.trainerclienthub.DAO.PaymentDAO;
import com.trainerclienthub.DAO.SessionDAO;
import com.trainerclienthub.DAO.WorkoutDAO;
import com.trainerclienthub.model.Client;
import com.trainerclienthub.model.Exercise;
import com.trainerclienthub.model.Session;
import com.trainerclienthub.model.SessionStatus;
import com.trainerclienthub.model.PaymentStatus;
import com.trainerclienthub.model.Workout;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReportService {
    // Generates reporting datasets across the app.


    private final ClientDAO   clientDAO;
    private final WorkoutDAO  workoutDAO;
    private final ExerciseDAO exerciseDAO;
    private final SessionDAO  sessionDAO;
    private final PaymentDAO  paymentDAO;

    private static final DateTimeFormatter WEEK_FMT =
            DateTimeFormatter.ofPattern("dd MMM");

    public ReportService() {
        this.clientDAO   = new ClientDAO();
        this.workoutDAO  = new WorkoutDAO();
        this.exerciseDAO = new ExerciseDAO();
        this.sessionDAO  = new SessionDAO();
        this.paymentDAO  = new PaymentDAO();
    }


    public Map<String, BigDecimal> getClientWorkoutProgress(
            int clientId, int weeks, LocalDate from, LocalDate to) {
        return getClientWorkoutProgress(clientId, weeks, from, to, null);
    }

    /** @param trainerId if non-null (TRAINER), restricts to workouts logged by that trainer */
    public Map<String, BigDecimal> getClientWorkoutProgress(
            int clientId, int weeks, LocalDate from, LocalDate to, Integer trainerId) {

        Map<String, BigDecimal> weeklyVolume = buildWeekSlots(weeks, to);

        List<Workout> workouts = workoutDAO.findByClientAndDateRange(
                clientId,
                Date.valueOf(from),
                Date.valueOf(to));

        if (trainerId != null) {
            workouts = workouts.stream().filter(w -> w.getTrainerId() == trainerId).toList();
        }

        for (Workout workout : workouts) {
            String weekLabel = weekLabelFor(workout.getWorkoutDate(), weeks, to);
            if (weekLabel == null) continue;

            List<Exercise> exercises = exerciseDAO.findByWorkout(workout.getWorkoutId());
            BigDecimal workoutVolume = exercises.stream()
                    .map(Exercise::getVolume)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            weeklyVolume.merge(weekLabel, workoutVolume, BigDecimal::add);
        }

        return weeklyVolume;
    }

    public List<Client> getAllClients() {
        return getAllClients(null);
    }

    /** @param trainerId if non-null (TRAINER), returns only that trainer's clients */
    public List<Client> getAllClients(Integer trainerId) {
        return trainerId != null ? clientDAO.findByTrainer(trainerId) : clientDAO.findAll();
    }


    public Map<String, BigDecimal> getGymWorkoutVolume(
            int weeks, LocalDate from, LocalDate to) {
        return getGymWorkoutVolume(weeks, from, to, null);
    }

    /** @param trainerId if non-null (TRAINER), restricts to workouts for that trainer's clients */
    public Map<String, BigDecimal> getGymWorkoutVolume(
            int weeks, LocalDate from, LocalDate to, Integer trainerId) {

        Map<String, BigDecimal> weeklyVolume = buildWeekSlots(weeks, to);

        List<Workout> allWorkouts = trainerId != null ? workoutDAO.findByTrainer(trainerId) : workoutDAO.findAll();

        for (Workout workout : allWorkouts) {
            LocalDate d = workout.getWorkoutDate();
            if (d.isBefore(from) || d.isAfter(to)) continue;

            String weekLabel = weekLabelFor(d, weeks, to);
            if (weekLabel == null) continue;

            List<Exercise> exercises = exerciseDAO.findByWorkout(workout.getWorkoutId());
            BigDecimal vol = exercises.stream()
                    .map(Exercise::getVolume)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            weeklyVolume.merge(weekLabel, vol, BigDecimal::add);
        }

        return weeklyVolume;
    }


    public Map<String, Integer> getMostActiveClients(
            LocalDate from, LocalDate to, int limit) {
        return getMostActiveClients(from, to, limit, null);
    }

    /** @param trainerId if non-null (TRAINER), restricts to sessions for that trainer only */
    public Map<String, Integer> getMostActiveClients(
            LocalDate from, LocalDate to, int limit, Integer trainerId) {

        if (limit <= 0) return new LinkedHashMap<>();

        Map<String, Integer> result = new LinkedHashMap<>();
        sessionDAO.findMostActiveClients(from, to, limit, trainerId)
                .forEach(entry -> result.put(entry.getKey(), entry.getValue()));
        return result;
    }


    public BigDecimal getTotalRevenue(LocalDate from, LocalDate to) {
        return paymentDAO.sumCompletedAmountByDateRange(Date.valueOf(from), Date.valueOf(to));
    }

    public Map<YearMonth, BigDecimal> getMonthlyRevenueTrend(LocalDate from, LocalDate to) {
        LinkedHashMap<YearMonth, BigDecimal> trend = new LinkedHashMap<>();
        YearMonth current = YearMonth.from(from);
        YearMonth end = YearMonth.from(to);
        while (!current.isAfter(end)) {
            trend.put(current, BigDecimal.ZERO);
            current = current.plusMonths(1);
        }

        paymentDAO.findByDateRange(Date.valueOf(from), Date.valueOf(to))
                .stream()
                .filter(p -> p.getPaymentStatus() == PaymentStatus.COMPLETED)
                .forEach(payment -> {
                    YearMonth month = YearMonth.from(payment.getPaymentDate());
                    trend.merge(month, payment.getAmount(), BigDecimal::add);
                });

        return trend;
    }

    public int getNewMembersCount(LocalDate from, LocalDate to) {
        return getNewMembersCount(from, to, null);
    }

    /** @param trainerId if non-null (TRAINER), restricts to that trainer's clients */
    public int getNewMembersCount(LocalDate from, LocalDate to, Integer trainerId) {
        List<Client> clients = trainerId != null ? clientDAO.findByTrainer(trainerId) : clientDAO.findAll();
        return (int) clients.stream()
                .filter(c -> c.getCreatedAt() != null)
                .filter(c -> {
                    LocalDate d = c.getCreatedAt().toLocalDate();
                    return !d.isBefore(from) && !d.isAfter(to);
                })
                .count();
    }

    public int getCompletedSessionsCount(LocalDate from, LocalDate to) {
        return getCompletedSessionsCount(from, to, null);
    }

    /** @param trainerId if non-null (TRAINER), restricts to sessions for that trainer only */
    public int getCompletedSessionsCount(LocalDate from, LocalDate to, Integer trainerId) {
        List<Session> sessions = trainerId != null ? sessionDAO.findByTrainer(trainerId) : sessionDAO.findAll();
        return (int) sessions.stream()
                .filter(s -> s.getStatus() == SessionStatus.COMPLETED)
                .filter(s -> !s.getSessionDate().isBefore(from)
                          && !s.getSessionDate().isAfter(to))
                .count();
    }

    public String getAvgWorkoutsPerMember(LocalDate from, LocalDate to) {
        return getAvgWorkoutsPerMember(from, to, null);
    }

    /** @param trainerId if non-null (TRAINER), restricts to that trainer's clients and workouts */
    public String getAvgWorkoutsPerMember(LocalDate from, LocalDate to, Integer trainerId) {
        List<Client> clients = trainerId != null ? clientDAO.findByTrainer(trainerId) : clientDAO.findAll();
        if (clients.isEmpty()) return "0";

        List<Workout> workouts = trainerId != null ? workoutDAO.findByTrainer(trainerId) : workoutDAO.findAll();
        long totalWorkouts = workouts.stream()
                .filter(w -> !w.getWorkoutDate().isBefore(from)
                          && !w.getWorkoutDate().isAfter(to))
                .count();

        double avg = (double) totalWorkouts / clients.size();
        return String.format("%.1f", avg);
    }


    private Map<String, BigDecimal> buildWeekSlots(int weeks, LocalDate to) {
        Map<String, BigDecimal> slots = new LinkedHashMap<>();
        LocalDate weekEnd = to;
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < weeks; i++) {
            LocalDate weekStart = weekEnd.minusDays(6);
            labels.add(weekStart.format(WEEK_FMT));
            weekEnd = weekEnd.minusDays(7);
        }
        Collections.reverse(labels);
        labels.forEach(l -> slots.put(l, BigDecimal.ZERO));
        return slots;
    }

    private String weekLabelFor(LocalDate date, int weeks, LocalDate to) {
        LocalDate slotEnd   = to;
        for (int i = 0; i < weeks; i++) {
            LocalDate slotStart = slotEnd.minusDays(6);
            if (!date.isBefore(slotStart) && !date.isAfter(slotEnd)) {
                return slotStart.format(WEEK_FMT);
            }
            slotEnd = slotEnd.minusDays(7);
        }
        return null;
    }
}
