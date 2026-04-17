package com.trainerclienthub.controller;

import com.trainerclienthub.db.DatabaseConnection;
import com.trainerclienthub.model.Client;
import com.trainerclienthub.model.TrainerRole;
import com.trainerclienthub.service.ReportService;
import com.trainerclienthub.util.SessionManager;
import com.trainerclienthub.util.ViewLoader;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class ReportController implements Initializable {

    //  FXML — top bar + sidebar

    @FXML
    private Label avatarLabel;
    @FXML
    private HBox navMemberships;
    @FXML
    private HBox navPayments;
    @FXML
    private HBox navTrainers;
    @FXML
    private DatePicker fromDate;
    @FXML
    private DatePicker toDate;
    @FXML
    private Button generateBtn;

    //  FXML — stat cards

    @FXML
    private VBox totalRevenueCard;
    @FXML
    private Label totalRevenueStat;
    @FXML
    private Label newMembersStat;
    @FXML
    private Label sessionsCompletedStat;
    @FXML
    private Label avgWorkoutsStat;

    // FXML  Chart 1: Client Workout Progress (LineChart)

    @FXML
    private ComboBox<Client> workoutClientFilter;
    @FXML
    private LineChart<String, Number> workoutProgressChart;

    // FXML  Chart 2: Gym Workout Volume (BarChart)

    @FXML
    private BarChart<String, Number> gymVolumeChart;

    //  FXML Chart 3: Most Active Clients (BarChart)

    @FXML
    private BarChart<String, Number> activeClientsChart;

    //  FXML legacy charts + gym-wide sections (hidden for TRAINER)

    @FXML
    private VBox gymVolumeChartSection;
    @FXML
    private VBox membershipDistributionSection;
    @FXML
    private VBox revenueTrendChartSection;
    @FXML
    private PieChart membershipPieChart;
    @FXML
    private LineChart<String, Number> revenueTrendChart;

    //  Service

    private final ReportService reportService = new ReportService();

    // State

    private static final int WEEKS = 8;

    private static final int LEADERBOARD_SIZE = 3;

    //  Initialise

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        populateAvatarLabel();
        applyRoleBasedUI();
        setDefaultDateRange();
        configureChartAxes();
        loadClientFilterComboBox();
        loadAllCharts();
    }

    private void applyRoleBasedUI() {
        boolean isTrainer = SessionManager.getInstance().getRole() == TrainerRole.TRAINER;
        if (navMemberships != null) {
            navMemberships.setVisible(!isTrainer);
            navMemberships.setManaged(!isTrainer);
        }
        if (navPayments != null) {
            navPayments.setVisible(!isTrainer);
            navPayments.setManaged(!isTrainer);
        }
        if (navTrainers != null) {
            navTrainers.setVisible(!isTrainer);
            navTrainers.setManaged(!isTrainer);
        }
        if (totalRevenueCard != null) {
            totalRevenueCard.setVisible(!isTrainer);
            totalRevenueCard.setManaged(!isTrainer);
        }
        if (gymVolumeChartSection != null) {
            gymVolumeChartSection.setVisible(!isTrainer);
            gymVolumeChartSection.setManaged(!isTrainer);
        }
        if (revenueTrendChartSection != null) {
            revenueTrendChartSection.setVisible(!isTrainer);
            revenueTrendChartSection.setManaged(!isTrainer);
        }
        if (membershipDistributionSection != null) {
            membershipDistributionSection.setVisible(!isTrainer);
            membershipDistributionSection.setManaged(!isTrainer);
        }
    }

    //  Handlers

    @FXML
    private void handleGenerate() {
        if (!validateDateRange()) return;
        loadAllCharts();
    }

    @FXML
    private void handleClientFilterChanged() {
        loadClientWorkoutProgressChart();
    }

    @FXML
    private void handleExportReport() {
        System.out.println("Export requested — PDF export not yet implemented.");
    }

    // Sidebar navigation

    @FXML
    private void handleNavDashboard(MouseEvent e) {
        navigateTo("DashboardView.fxml", "TCH — Dashboard");
    }

    @FXML
    private void handleNavClients(MouseEvent e) {
        navigateTo("ClientManagementView.fxml", "TCH — Clients");
    }

    @FXML
    private void handleNavWorkouts(MouseEvent e) {
        navigateTo("WorkoutTrackingView.fxml", "TCH — Workouts");
    }

    @FXML
    private void handleNavMemberships(MouseEvent e) {
        navigateTo("MembershipManagementView.fxml", "TCH — Memberships");
    }

    @FXML
    private void handleNavSessions(MouseEvent e) {
        navigateTo("SessionManagementView.fxml", "TCH — Sessions");
    }

    @FXML
    private void handleNavPayments(MouseEvent e) {
        navigateTo("Payments.fxml", "TCH — Payments");
    }

    @FXML
    private void handleNavTrainers(MouseEvent e) {
        navigateTo("Trainers.fxml", "TCH — Trainers");
    }

    @FXML
    private void handleLogout(MouseEvent e) {
        SessionManager.getInstance().logout();
        Stage stage = (Stage) generateBtn.getScene().getWindow();
        ViewLoader.navigateTo(stage, "LoginView.fxml", "TCH — Login");
    }

    // Data loading

    private void loadAllCharts() {
        LocalDate from = fromDate.getValue();
        LocalDate to = toDate.getValue();
        Integer trainerId = getTrainerIdForDataScope();

        populateStatCards(from, to, trainerId);
        loadClientWorkoutProgressChart(trainerId);
        loadGymVolumeChart(from, to, trainerId);
        loadMostActiveClientsChart(from, to, trainerId);
        loadRevenueTrendChart(from, to);
        loadMembershipDistributionChart();
    }

    private Integer getTrainerIdForDataScope() {
        if (SessionManager.getInstance().getRole() != TrainerRole.TRAINER) return null;
        var t = SessionManager.getInstance().getCurrentTrainer();
        return t != null ? t.getTrainerId() : null;
    }

    //  Chart 1: Client Workout Progress (LineChart)

    private void loadClientWorkoutProgressChart() {
        loadClientWorkoutProgressChart(getTrainerIdForDataScope());
    }

    private void loadClientWorkoutProgressChart(Integer trainerId) {
        workoutProgressChart.getData().clear();

        Client selected = workoutClientFilter.getValue();
        LocalDate to = toDate.getValue();
        LocalDate from = to.minusWeeks(WEEKS);

        XYChart.Series<String, Number> series = new XYChart.Series<>();

        if (selected == null) {
            // No client chosen — seed empty slots so the chart renders cleanly
            series.setName("Select a client");
            for (int i = WEEKS - 1; i >= 0; i--) {
                String label = to.minusWeeks(i).minusDays(6)
                        .format(DateTimeFormatter.ofPattern("dd MMM"));
                series.getData().add(new XYChart.Data<>(label, 0));
            }
        } else {
            series.setName(selected.getName());
            Map<String, BigDecimal> weeklyVolume = reportService.getClientWorkoutProgress(
                    selected.getClientId(), WEEKS, from, to, trainerId);

            weeklyVolume.forEach((weekLabel, volume) ->
                    series.getData().add(
                            new XYChart.Data<>(weekLabel, volume)));
        }

        workoutProgressChart.getData().add(series);
        applyNeonLineStyle(workoutProgressChart);
    }

    //  Chart 2: Gym Workout Volume (BarChart)

    private void loadGymVolumeChart(LocalDate from, LocalDate to, Integer trainerId) {
        gymVolumeChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Total Volume (kg)");

        Map<String, BigDecimal> gymVolume = reportService.getGymWorkoutVolume(WEEKS, from, to, trainerId);

        gymVolume.forEach((weekLabel, volume) ->
                series.getData().add(
                        new XYChart.Data<>(weekLabel, volume)));

        gymVolumeChart.getData().add(series);
        applyNeonBarStyle(gymVolumeChart);
    }

    //  Chart 3: Most Active Clients (BarChart)

    private void loadMostActiveClientsChart(LocalDate from, LocalDate to, Integer trainerId) {
        activeClientsChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Completed Sessions");

        Map<String, Integer> topClients = reportService.getMostActiveClients(from, to, LEADERBOARD_SIZE, trainerId);
        ObservableList<String> categories = FXCollections.observableArrayList();

        if (topClients.isEmpty()) {
            series.getData().add(new XYChart.Data<>("No data", 0));
            categories.add("No data");
        } else {
            topClients.forEach((name, count) -> {
                series.getData().add(new XYChart.Data<>(name, count));
                categories.add(name);
            });
        }

        CategoryAxis actX = (CategoryAxis) activeClientsChart.getXAxis();
        actX.setCategories(categories);

        activeClientsChart.getData().add(series);
        applyNeonBarStyle(activeClientsChart);
    }

    private void loadRevenueTrendChart(LocalDate from, LocalDate to) {
        revenueTrendChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Revenue (NPR)");

        Map<YearMonth, BigDecimal> trend = reportService.getMonthlyRevenueTrend(from, to);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy");

        trend.forEach((month, amount) -> {
            BigDecimal value = amount != null ? amount : BigDecimal.ZERO;
            series.getData().add(new XYChart.Data<>(month.format(formatter), value));
        });

        if (trend.isEmpty()) {
            series.getData().add(new XYChart.Data<>("No data", 0));
        }

        revenueTrendChart.getData().add(series);
        applyNeonLineStyle(revenueTrendChart);
    }

    private void loadMembershipDistributionChart() {
        if (membershipPieChart == null) return;

        Task<ObservableList<PieChart.Data>> pieTask = new Task<>() {
            @Override
            protected ObservableList<PieChart.Data> call() {
                ObservableList<PieChart.Data> slices = FXCollections.observableArrayList();
                String sql = """
                        SELECT p.plan_name,
                               COUNT(m.membership_id) AS plan_count
                          FROM membership_plan p
                          LEFT JOIN membership m ON p.plan_id = m.plan_id AND UPPER(m.status) = 'ACTIVE'
                         GROUP BY p.plan_name
                         ORDER BY plan_count DESC
                        """;
                try (Connection conn = DatabaseConnection.getInstance().getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql);
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        slices.add(new PieChart.Data(
                                rs.getString("plan_name"),
                                rs.getInt("plan_count")));
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
                return slices;
            }
        };

        pieTask.setOnSucceeded(event -> Platform.runLater(() -> {
            ObservableList<PieChart.Data> data = pieTask.getValue();
            membershipPieChart.setData(data);
            membershipPieChart.setStartAngle(90);

            // This is what makes the Pie Chart physically bigger!
            membershipPieChart.setLabelsVisible(false);
            membershipPieChart.setLegendVisible(true);

            String[] neon = {"#CCFF00", "#00E5FF", "#B300FF", "#FF5F00", "#00FFA5"};

            // Force the UI to build so the legend dots actually exist before we color them
            membershipPieChart.applyCss();
            membershipPieChart.layout();

            // 1. Color the slices and add tooltips
            for (int i = 0; i < data.size(); i++) {
                PieChart.Data slice = data.get(i);
                String color = neon[i % neon.length];
                Runnable decorate = () -> {
                    Node node = slice.getNode();
                    if (node != null) {
                        node.setStyle("-fx-pie-color: " + color + ";");
                        Tooltip.install(node, new Tooltip(slice.getName() + ": " + (int) slice.getPieValue()));
                    }
                };
                if (slice.getNode() != null) {
                    decorate.run();
                } else {
                    slice.nodeProperty().addListener((obs, oldNode, node) -> {
                        if (node != null) {
                            decorate.run();
                        }
                    });
                }
            }

            // 2. Color the Legend Dots safely
            int index = 0;
            for (Node legendSymbol : membershipPieChart.lookupAll(".chart-legend-item-symbol")) {
                if (index < data.size()) {
                    legendSymbol.setStyle("-fx-background-color: " + neon[index % neon.length] + ", transparent;");
                    index++;
                }
            }
        }));

        pieTask.setOnFailed(event -> {
            if (pieTask.getException() != null) {
                pieTask.getException().printStackTrace();
            }
        });


        new Thread(pieTask, "membership-distribution-loader").start();
    }

    //  Stat cards

    private void populateStatCards(LocalDate from, LocalDate to, Integer trainerId) {
        // Revenue (only shown for ADMIN; card hidden for TRAINER)
        if (trainerId == null && totalRevenueStat != null) {
            BigDecimal revenue = reportService.getTotalRevenue(from, to);
            totalRevenueStat.setText("Rs " + String.format("%,.0f", revenue));
        }

        // New members
        int newMembers = reportService.getNewMembersCount(from, to, trainerId);
        newMembersStat.setText(String.valueOf(newMembers));

        // Completed sessions
        int completed = reportService.getCompletedSessionsCount(from, to, trainerId);
        sessionsCompletedStat.setText(String.valueOf(completed));

        // Avg workouts per member
        String avg = reportService.getAvgWorkoutsPerMember(from, to, trainerId);
        avgWorkoutsStat.setText(avg);
    }

    // Setup helpers

    private void populateAvatarLabel() {
        var trainer = SessionManager.getInstance().getCurrentTrainer();
        if (trainer != null && !trainer.getName().isBlank()) {
            avatarLabel.setText(String.valueOf(trainer.getName().charAt(0)).toUpperCase());
        }
    }

    private void setDefaultDateRange() {
        toDate.setValue(LocalDate.now());
        fromDate.setValue(LocalDate.now().minusWeeks(WEEKS));
    }

    private void configureChartAxes() {
        // Chart 1 — Client Workout Progress (LineChart)
        workoutProgressChart.setAnimated(false);
        workoutProgressChart.setLegendVisible(false);
        workoutProgressChart.setCreateSymbols(true);

        CategoryAxis lineX = (CategoryAxis) workoutProgressChart.getXAxis();
        NumberAxis lineY = (NumberAxis) workoutProgressChart.getYAxis();
        lineX.setLabel("Week");
        lineY.setLabel("Volume (kg)");
        lineX.setTickLabelRotation(-35);

        // Chart 2 — Gym Volume (BarChart)
        gymVolumeChart.setAnimated(false);
        gymVolumeChart.setLegendVisible(false);

        CategoryAxis gymX = (CategoryAxis) gymVolumeChart.getXAxis();
        NumberAxis gymY = (NumberAxis) gymVolumeChart.getYAxis();
        gymX.setLabel("Week");
        gymY.setLabel("Volume (kg)");
        gymX.setTickLabelRotation(-35);

        // Chart 3 — Most Active Clients (BarChart)
        activeClientsChart.setAnimated(false);
        activeClientsChart.setLegendVisible(false);

        CategoryAxis actX = (CategoryAxis) activeClientsChart.getXAxis();
        NumberAxis actY = (NumberAxis) activeClientsChart.getYAxis();
        actX.setLabel("Client");
        actY.setLabel("Sessions");
        actX.setTickLabelRotation(-25);
    }

    private void loadClientFilterComboBox() {
        List<Client> clients = reportService.getAllClients(getTrainerIdForDataScope());
        ObservableList<Client> items = FXCollections.observableArrayList(clients);
        workoutClientFilter.setItems(items);

        // Show client name in the dropdown
        workoutClientFilter.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Client item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });
        workoutClientFilter.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Client item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "All clients" : item.getName());
            }
        });

        workoutClientFilter.setOnAction(e -> handleClientFilterChanged());
    }

    private void applyNeonLineStyle(LineChart<String, Number> chart) {
        chart.applyCss();
        chart.layout();

        chart.lookupAll(".chart-series-line").forEach(node ->
                node.setStyle("-fx-stroke: #CCFF00; -fx-stroke-width: 2.5px;"));

        chart.lookupAll(".chart-line-symbol").forEach(node ->
                node.setStyle("-fx-background-color: #CCFF00, #121212; " +
                        "-fx-background-radius: 5px; " +
                        "-fx-padding: 4px;"));
    }

    private void applyNeonBarStyle(BarChart<String, Number> chart) {
        chart.applyCss();
        chart.layout();

        chart.lookupAll(".chart-bar").forEach(node ->
                node.setStyle("-fx-bar-fill: #CCFF00;"));
    }

    private boolean validateDateRange() {
        LocalDate from = fromDate.getValue();
        LocalDate to = toDate.getValue();

        if (from == null || to == null) {
            fromDate.setStyle("-fx-border-color: #FF4444;");
            toDate.setStyle("-fx-border-color: #FF4444;");
            return false;
        }

        if (from.isAfter(to)) {
            fromDate.setStyle("-fx-border-color: #FF4444;");
            return false;
        }

        // Reset borders
        fromDate.setStyle("");
        toDate.setStyle("");
        return true;
    }

    private void navigateTo(String fxml, String title) {
        Stage stage = (Stage) generateBtn.getScene().getWindow();
        ViewLoader.navigateTo(stage, fxml, title);
    }
}