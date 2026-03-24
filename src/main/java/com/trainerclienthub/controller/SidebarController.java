package com.trainerclienthub.controller;

import com.trainerclienthub.model.TrainerRole;
import com.trainerclienthub.util.SessionManager;
import com.trainerclienthub.util.ViewLoader;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class SidebarController implements Initializable {

    @FXML private HBox navDashboard;
    @FXML private HBox navClients;
    @FXML private HBox navWorkouts;
    @FXML private HBox navMemberships;
    @FXML private HBox navSessions;
    @FXML private HBox navPayments;
    @FXML private Button trainersButton;
    @FXML private HBox navReports;

    private final Map<String, Node> navItems = new LinkedHashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        navItems.put("dashboard", navDashboard);
        navItems.put("clients", navClients);
        navItems.put("workouts", navWorkouts);
        navItems.put("memberships", navMemberships);
        navItems.put("sessions", navSessions);
        navItems.put("payments", navPayments);
        navItems.put("trainers", trainersButton);
        navItems.put("reports", navReports);
        applyAccessControl();
    }

    private void applyAccessControl() {
        boolean hideTrainers = SessionManager.getInstance().getRole() == TrainerRole.TRAINER;
        if (trainersButton != null) {
            trainersButton.setVisible(!hideTrainers);
            trainersButton.setManaged(!hideTrainers);
        }
    }

    public void highlight(String key) {
        if (key == null) return;
        navItems.values().forEach(node -> {
            if (node != null) {
                node.getStyleClass().remove("nav-item-active");
            }
        });
        Node active = navItems.get(key);
        if (active != null) {
            if (!active.getStyleClass().contains("nav-item-active")) {
                active.getStyleClass().add("nav-item-active");
            }
        }
    }

    @FXML
    private void handleNavDashboard(MouseEvent event) {
        navigateTo(event, "DashboardView.fxml", "TCH — Dashboard");
    }

    @FXML
    private void handleNavClients(MouseEvent event) {
        navigateTo(event, "ClientManagementView.fxml", "TCH — Clients");
    }

    @FXML
    private void handleNavWorkouts(MouseEvent event) {
        navigateTo(event, "WorkoutTrackingView.fxml", "TCH — Workouts");
    }

    @FXML
    private void handleNavMemberships(MouseEvent event) {
        navigateTo(event, "MembershipManagementView.fxml", "TCH — Memberships");
    }

    @FXML
    private void handleNavSessions(MouseEvent event) {
        navigateTo(event, "SessionManagementView.fxml", "TCH — Sessions");
    }

    @FXML
    private void handleNavPayments(MouseEvent event) {
        navigateTo(event, "Payments.fxml", "TCH — Payments");
    }

    @FXML
    private void handleNavTrainers(ActionEvent event) {
        navigateTo(event, "Trainers.fxml", "TCH — Trainers");
    }

    @FXML
    private void handleNavReports(MouseEvent event) {
        navigateTo(event, "ReportsView.fxml", "TCH — Reports");
    }

    @FXML
    private void handleLogout(MouseEvent event) {
        SessionManager.getInstance().logout();
        navigateTo(event, "LoginView.fxml", "TCH — Login");
    }

    private void navigateTo(MouseEvent event, String fxml, String title) {
        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        ViewLoader.navigateTo(stage, fxml, title);
    }

    private void navigateTo(ActionEvent event, String fxml, String title) {
        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        ViewLoader.navigateTo(stage, fxml, title);
    }
}
