package com.trainerclienthub.controller;

import com.trainerclienthub.model.Payment;
import com.trainerclienthub.model.Trainer;
import com.trainerclienthub.service.DashboardService;
import com.trainerclienthub.util.SessionManager;
import com.trainerclienthub.util.ViewLoader;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    //  FXML injections — top bar

    @FXML private Label greetingLabel;
    @FXML private Label avatarLabel;
    @FXML private Label dateLabel;

    // FXML injections — sidebar nav
    @FXML private HBox navClients;
    @FXML private HBox navWorkouts;
    @FXML private HBox navMemberships;
    @FXML private HBox navSessions;
    @FXML private HBox navPayments;
    @FXML private HBox navReports;

    //  FXML injections — summary cards

    @FXML private Label totalMembersLabel;
    @FXML private Label totalMembersTrendLabel;
    @FXML private Label activeMembershipsLabel;
    @FXML private Label membershipsTrendLabel;
    @FXML private Label sessionsTodayLabel;
    @FXML private Label sessionsCompletedLabel;
    @FXML private Label revenueLabel;
    @FXML private Label revenueTrendLabel;

    //  FXML injections — revenue + recent payments section

    @FXML private Label                        totalRevenueLabel;
    @FXML private TableView<Payment>           recentPaymentsTable;
    @FXML private TableColumn<Payment, String> colRecentClient;
    @FXML private TableColumn<Payment, String> colRecentAmount;
    @FXML private TableColumn<Payment, String> colRecentDate;

    //  FXML injections — chart + progress

    @FXML private LineChart<String, Number> membershipChart;
    @FXML private ProgressIndicator         utilizationIndicator;
    @FXML private Label                     utilizationLabel;
    @FXML private Label                     activeCountLabel;
    @FXML private Label                     totalCountLabel;

    //  Services

    private final DashboardService dashboardService = new DashboardService();

    //  Initialise

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        populateTrainerInfo();
        applyRoleBasedUI();
        populateSummaryCards();
        populateMembershipChart();
        populateUtilizationIndicator();
        populateRecentPayments();
    }

    // Data population

    /**
     * Shows or hides sidebar nav items based on the logged-in trainer's role.
     */
    private void applyRoleBasedUI() {
        boolean isAdmin = SessionManager.getInstance().isAdmin();

        // Memberships, Payments and Reports are admin-only
        setNavVisible(navMemberships, isAdmin);
        setNavVisible(navPayments,    isAdmin);
        setNavVisible(navReports,     isAdmin);

        // Clients, Workouts, Sessions visible to all roles
        setNavVisible(navClients,  true);
        setNavVisible(navWorkouts, true);
        setNavVisible(navSessions, true);
    }

    // Shows or hides a sidebar HBox and removes it from layout when hidden.
    private void setNavVisible(HBox nav, boolean visible) {
        if (nav == null) return;
        nav.setVisible(visible);
        nav.setManaged(visible);
    }

    /**
     * Shows the trainer's name in the greeting and their initial + role badge
     */
    private void populateTrainerInfo() {
        dateLabel.setText(LocalDate.now()
                .format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")));

        Trainer trainer = SessionManager.getInstance().getCurrentTrainer();
        if (trainer == null) return;

        String name  = trainer.getName();
        String role  = trainer.isAdmin() ? "Admin" : "Trainer";
        greetingLabel.setText("Welcome back, " + name.split(" ")[0] + "  ·  " + role);
        avatarLabel.setText(String.valueOf(name.charAt(0)).toUpperCase());
    }

    private void populateSummaryCards() {
        // Total members
        int total = dashboardService.getTotalClients();
        totalMembersLabel.setText(String.valueOf(total));

        int newThisMonth = dashboardService.getNewClientsThisMonth();
        totalMembersTrendLabel.setText("+" + newThisMonth + " this month");

        // Active memberships
        int active = dashboardService.getActiveMembershipCount();
        activeMembershipsLabel.setText(String.valueOf(active));

        int expiringSoon = dashboardService.getExpiringSoonCount(7);
        membershipsTrendLabel.setText(expiringSoon + " expiring in 7 days");

        // Sessions today
        int sessionsToday = dashboardService.getSessionsTodayCount();
        sessionsTodayLabel.setText(String.valueOf(sessionsToday));

        int completedToday = dashboardService.getSessionsCompletedTodayCount();
        sessionsCompletedLabel.setText(completedToday + " completed");

        // Revenue
        BigDecimal revenue  = dashboardService.getMonthlyRevenue();
        BigDecimal prevRev  = dashboardService.getPreviousMonthRevenue();
        revenueLabel.setText("Rs " + formatRevenue(revenue));

        String trend = buildRevenueTrend(revenue, prevRev);
        revenueTrendLabel.setText(trend);
        boolean positive = !trend.startsWith("-");
        revenueTrendLabel.getStyleClass().removeAll("card-trend-up", "card-trend-down");
        revenueTrendLabel.getStyleClass().add(positive ? "card-trend-up" : "card-trend-down");
    }

    /**
     * Builds a 6-month membership growth series and populates the {@link LineChart}.
     */
    private void populateMembershipChart() {
        membershipChart.getData().clear();
        membershipChart.setAnimated(false);

        int[] counts = dashboardService.getMembershipGrowthData(6);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("New Members");

        for (int i = 0; i < 6; i++) {
            YearMonth ym = YearMonth.now().minusMonths(5 - i);
            String monthLabel = ym.format(DateTimeFormatter.ofPattern("MMM yy"));
            series.getData().add(new XYChart.Data<>(monthLabel, counts[i]));
        }

        membershipChart.getData().add(series);

        // Apply neon line colour after data is committed to the scene
        membershipChart.applyCss();
        membershipChart.layout();
    }

    /**
     * Calculates active / total ratio and updates the progress indicator.
     */
    private void populateUtilizationIndicator() {
        int total  = dashboardService.getTotalClients();
        int active = dashboardService.getActiveMembershipCount();

        activeCountLabel.setText(String.valueOf(active));
        totalCountLabel.setText(String.valueOf(total));

        if (total == 0) {
            utilizationIndicator.setProgress(0);
            utilizationLabel.setText("0%");
            return;
        }

        double ratio      = (double) active / total;
        int    percentage = (int) Math.round(ratio * 100);

        utilizationIndicator.setProgress(ratio);
        utilizationLabel.setText(percentage + "%");
    }

    // Sidebar navigation

    @FXML private void handleNavClients(MouseEvent event) {
        navigateTo("ClientManagementView.fxml", "TCH — Clients");
    }

    @FXML private void handleNavWorkouts(MouseEvent event) {
        navigateTo("WorkoutTrackingView.fxml", "TCH — Workouts");
    }

    @FXML private void handleNavMemberships(MouseEvent event) {
        navigateTo("MembershipManagementView.fxml", "TCH — Memberships");
    }

    @FXML private void handleNavSessions(MouseEvent event) {
        navigateTo("SessionManagementView.fxml", "TCH — Sessions");
    }

    @FXML private void handleNavPayments(MouseEvent event) {
        navigateTo("PaymentManagementView.fxml", "TCH — Payments");
    }

    @FXML private void handleNavReports(MouseEvent event) {
        navigateTo("ReportsView.fxml", "TCH — Reports");
    }

    @FXML
    private void handleLogout(MouseEvent event) {
        SessionManager.getInstance().logout();
        Stage stage = (Stage) greetingLabel.getScene().getWindow();
        ViewLoader.navigateTo(stage, "LoginView.fxml", "TCH — Login");
    }

    //  Private helpers

    private void navigateTo(String fxml, String title) {
        Stage stage = (Stage) greetingLabel.getScene().getWindow();
        ViewLoader.navigateTo(stage, fxml, title);
    }

    /**
     * Configures the recent-payments TableView columns and loads the last 5 payments.
     * Client names are resolved from clientId via DashboardService/ClientDAO.
     */
    private void populateRecentPayments() {
        if (recentPaymentsTable == null) return;   // guard if FXML not yet updated

        // Client name column — resolve clientId → name
        colRecentClient.setCellValueFactory(data -> {
            int clientId = data.getValue().getClientId();
            try {
                com.trainerclienthub.DAO.ClientDAO dao =
                        new com.trainerclienthub.DAO.ClientDAO();
                return dao.findById(clientId)
                        .map(c -> new SimpleStringProperty(c.getName()))
                        .orElse(new SimpleStringProperty("Client #" + clientId));
            } catch (Exception e) {
                return new SimpleStringProperty("—");
            }
        });

        // Amount column
        colRecentAmount.setCellValueFactory(data ->
                new SimpleStringProperty("Rs " + data.getValue().getAmount()
                        .setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()));

        // Date column
        colRecentDate.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getPaymentDate()
                        .format(DateTimeFormatter.ofPattern("dd MMM yyyy"))));

        List<Payment> recent = dashboardService.getRecentPayments(5);
        recentPaymentsTable.setItems(FXCollections.observableArrayList(recent));

        // Update the total revenue label
        if (totalRevenueLabel != null) {
            BigDecimal total = dashboardService.getTotalRevenue();
            totalRevenueLabel.setText("Rs " + formatRevenue(total));
        }
    }


    private String formatRevenue(BigDecimal amount) {
        if (amount == null) return "0";
        return String.format("%,.0f", amount.setScale(0, RoundingMode.HALF_UP));
    }

    /**
     * Builds a trend string comparing this month's revenue to last month's.
     * Returns e.g. "+12% vs last month" or "-5% vs last month".
     */
    private String buildRevenueTrend(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return "No data for last month";
        }
        BigDecimal diff    = current.subtract(previous);
        BigDecimal percent = diff.divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP);
        String sign = percent.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
        return sign + percent + "% vs last month";
    }
}