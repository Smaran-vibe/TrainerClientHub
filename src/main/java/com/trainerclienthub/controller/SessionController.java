package com.trainerclienthub.controller;

import com.trainerclienthub.model.Client;
import com.trainerclienthub.model.Session;
import com.trainerclienthub.model.TrainerRole;
import com.trainerclienthub.model.SessionStatus;
import com.trainerclienthub.model.Trainer;
import com.trainerclienthub.service.ClientService;
import com.trainerclienthub.service.SessionService;
import com.trainerclienthub.service.TrainerService;
import com.trainerclienthub.util.SessionManager;
import com.trainerclienthub.util.ViewLoader;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class SessionController implements Initializable {

    
    @FXML private Label avatarLabel;
    @FXML private HBox navMemberships;
    @FXML private HBox navPayments;
    @FXML private HBox navTrainers;

    
    @FXML private Label scheduledTodayLabel;
    @FXML private Label completedTodayLabel;
    @FXML private Label cancelledTodayLabel;
    @FXML private Label weeklyTotalLabel;

    
    @FXML private TextField          sessionSearchField;
    @FXML private DatePicker         dateFilter;
    @FXML private ComboBox<String>   statusFilterCombo;
    @FXML private Button             scheduleSessionBtn;
    @FXML private Button             completeSessionBtn;
    @FXML private Button             cancelSessionBtn;

    
    @FXML private TableView<Session>               sessionTable;
    @FXML private TableColumn<Session, Integer>    colSessionId;
    @FXML private TableColumn<Session, String>     colSessionClient;
    @FXML private TableColumn<Session, String>     colSessionTrainer;
    @FXML private TableColumn<Session, LocalDate>  colSessionDate;
    @FXML private TableColumn<Session, LocalTime>  colSessionTime;
    @FXML private TableColumn<Session, String>     colSessionStatus;
    @FXML private TableColumn<Session, String>     colSessionNotes;

    
    @FXML private VBox sessionFormPanel;
    @FXML private Label             sessionFormTitle;
    @FXML private ComboBox<Client>  fSessionClient;
    @FXML private DatePicker        fSessionDate;
    @FXML private TextField         fSessionTime;
    @FXML private TextArea          fSessionNotes;
    @FXML private VBox              balancePreviewBox;
    @FXML private Label             sessionBalancePreview;
    @FXML private Label             sessionFormErrorLabel;
    @FXML private Button            saveSessionBtn;

    
    private final SessionService sessionService = new SessionService();
    private final ClientService  clientService  = new ClientService();
    private final TrainerService trainerService = new TrainerService();

    private final ObservableList<Session> allSessions = FXCollections.observableArrayList();
    private FilteredList<Session> filteredSessions;

    

    @Override
    // Setup table/filters and load initial session list
    public void initialize(URL location, ResourceBundle resources) {
        populateAvatarLabel();
        applyRoleBasedUI();
        configureTable();
        loadSessions();
        filteredSessions = new FilteredList<>(allSessions, session -> true);
        sessionTable.setItems(filteredSessions);
        populateStatCards();
        statusFilterCombo.setItems(FXCollections.observableArrayList(
                "SCHEDULED",
                "COMPLETED",
                "CANCELLED"));
        statusFilterCombo.setValue("SCHEDULED");
        wireFilters();
        applyFilters();
        loadClientFormComboBox();
        hideFormPanel();
        hideFormError();
    }

    // Trainers have a limited sidebar compared to admins
    private void applyRoleBasedUI() {
        boolean isTrainer = SessionManager.getInstance().getRole() == TrainerRole.TRAINER;
        if (navMemberships != null) { navMemberships.setVisible(!isTrainer); navMemberships.setManaged(!isTrainer); }
        if (navPayments != null)    { navPayments.setVisible(!isTrainer);    navPayments.setManaged(!isTrainer); }
        if (navTrainers != null)    { navTrainers.setVisible(!isTrainer);    navTrainers.setManaged(!isTrainer); }
    }

    

    private void configureTable() {
        colSessionId.setCellValueFactory(new PropertyValueFactory<>("sessionId"));
        colSessionDate.setCellValueFactory(new PropertyValueFactory<>("sessionDate"));
        colSessionTime.setCellValueFactory(new PropertyValueFactory<>("sessionTime"));
        colSessionNotes.setCellValueFactory(new PropertyValueFactory<>("notes"));

        colSessionClient.setCellValueFactory(data -> {
            Optional<Client> c = clientService.findById(data.getValue().getClientId());
            return new javafx.beans.property.SimpleStringProperty(
                    c.map(Client::getName).orElse("Unknown"));
        });

        colSessionTrainer.setCellValueFactory(new PropertyValueFactory<>("trainerName"));

        colSessionStatus.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getStatus().name()));

        colSessionStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle(switch (item) {
                    case "SCHEDULED"  -> "-fx-text-fill:#FFAA00; -fx-font-weight:bold;";
                    case "COMPLETED"  -> "-fx-text-fill:#00CC66; -fx-font-weight:bold;";
                    case "CANCELLED"  -> "-fx-text-fill:#FF4444;";
                    case "NO_SHOW"    -> "-fx-text-fill:#AAAAAA;";
                    default           -> "";
                });
            }
        });

    }

    // Refresh sessions and re-apply current filters
    private void loadSessions() {
        allSessions.setAll(sessionService.findAll());
        if (filteredSessions != null) {
            applyFilters();
        }
    }

    

    private void populateStatCards() {
        List<Session> sessions = new ArrayList<>(allSessions);

        long scheduled = sessions.stream().filter(s -> s.getStatus() == SessionStatus.SCHEDULED).count();
        long completed = sessions.stream().filter(s -> s.getStatus() == SessionStatus.COMPLETED).count();
        long cancelled = sessions.stream().filter(s -> s.getStatus() == SessionStatus.CANCELLED).count();

        scheduledTodayLabel.setText(String.valueOf(scheduled));
        completedTodayLabel.setText(String.valueOf(completed));
        cancelledTodayLabel.setText(String.valueOf(cancelled));

        
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(java.time.DayOfWeek.MONDAY);
        long weekly = sessions.stream()
                .filter(s -> !s.getSessionDate().isBefore(monday)
                        && !s.getSessionDate().isAfter(today))
                .count();
        weeklyTotalLabel.setText(String.valueOf(weekly));
    }

    

    private void wireFilters() {
        sessionSearchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        dateFilter.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        statusFilterCombo.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters());
    }

    @FXML
    private void handleDateFilter(ActionEvent event) {
        applyFilters();
    }

    private void applyFilters() {
        if (filteredSessions == null) return;

        String keyword = sessionSearchField.getText() == null
                ? ""
                : sessionSearchField.getText().trim().toLowerCase();
        String status  = statusFilterCombo.getValue();
        LocalDate date = dateFilter.getValue();

        filteredSessions.setPredicate(session -> {
            boolean statusMatches = status == null || status.isBlank()
                    || session.getStatus().name().equalsIgnoreCase(status);

            boolean dateMatches = date == null || session.getSessionDate().equals(date);
            boolean searchMatches = keyword.isEmpty()
                    || matchesClientOrTrainer(session, keyword)
                    || String.valueOf(session.getSessionId()).contains(keyword)
                    || (session.getNotes() != null && session.getNotes().toLowerCase().contains(keyword));

            return statusMatches && dateMatches && searchMatches;
        });
        sessionTable.setItems(filteredSessions);
    }

    private boolean matchesClientOrTrainer(Session session, String keyword) {
        Optional<Client> clientOpt = clientService.findById(session.getClientId());
        if (clientOpt.map(client -> client.getName().toLowerCase().contains(keyword)).orElse(false)) {
            return true;
        }

        Optional<Trainer> trainerOpt = trainerService.findById(session.getTrainerId());
        if (trainerOpt.map(trainer -> trainer.getName().toLowerCase().contains(keyword)).orElse(false)) {
            return true;
        }

        String fallbackTrainer = ("trainer #" + session.getTrainerId()).toLowerCase();
        return fallbackTrainer.contains(keyword);
    }

    

    @FXML
    private void handleScheduleSession(ActionEvent event) {
        sessionFormTitle.setText("Schedule Session");
        fSessionDate.setValue(LocalDate.now().plusDays(1));
        fSessionTime.setText("08:00");
        fSessionNotes.clear();
        fSessionClient.setValue(null);
        balancePreviewBox.setVisible(false); balancePreviewBox.setManaged(false);
        hideFormError();
        showFormPanel();
    }

    @FXML
    private void handleCompleteSession(ActionEvent event) {
        Session selected = sessionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection",
                    "Please select a session to mark complete.");
            return;
        }
        try {
            sessionService.completeSession(selected.getSessionId());
            loadSessions();
            populateStatCards();
            showAlert(Alert.AlertType.INFORMATION, "Session Completed",
                    "Session marked as completed. "
                            + "Client's session balance has been decremented.");
        } catch (IllegalStateException ex) {
            showAlert(Alert.AlertType.ERROR, "Cannot Complete", ex.getMessage());
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Error", ex.getMessage());
        }
    }

    @FXML
    private void handleCancelSession(ActionEvent event) {
        Session selected = sessionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection",
                    "Please select a session to cancel.");
            return;
        }

        Optional<ButtonType> confirm = showConfirm("Cancel Session",
                "Cancel session on " + selected.getSessionDate() + "?",
                "The session balance will NOT be deducted for a cancellation.");
        if (confirm.isPresent() && confirm.get() == ButtonType.OK) {
            try {
                sessionService.cancelSession(selected.getSessionId());
                loadSessions();
                populateStatCards();
                showAlert(Alert.AlertType.INFORMATION, "Session Cancelled",
                        "Session cancelled. No session balance was deducted.");
            } catch (IllegalStateException ex) {
                showAlert(Alert.AlertType.ERROR, "Cannot Cancel", ex.getMessage());
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Error", ex.getMessage());
            }
        }
    }

    

    private void loadClientFormComboBox() {
        fSessionClient.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Client c, boolean empty) {
                super.updateItem(c, empty);
                setText(empty || c == null ? null : c.getName());
            }
        });
        fSessionClient.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Client c, boolean empty) {
                super.updateItem(c, empty);
                setText(empty || c == null ? "Select client..." : c.getName());
            }
        });
        fSessionClient.setItems(FXCollections.observableArrayList(clientService.findAll()));
        fSessionClient.setOnAction(e -> {
            Client c = fSessionClient.getValue();
            if (c != null) {
                sessionBalancePreview.setText(c.getSessionBalance() + " remaining");
                balancePreviewBox.setVisible(true); balancePreviewBox.setManaged(true);
            }
        });
    }

    @FXML
    // Save form changes (add or edit session)
    private void handleSaveSession(ActionEvent event) {
        hideFormError();
        Client client = fSessionClient.getValue();
        LocalDate date = fSessionDate.getValue();
        String timeStr = fSessionTime.getText();

        if (client == null) { showFormError("Please select a client."); return; }
        if (date   == null) { showFormError("Please select a session date."); return; }
        if (timeStr == null || timeStr.isBlank()) {
            showFormError("Please enter a session time (HH:MM)."); return;
        }

        LocalTime time;
        try { time = LocalTime.parse(timeStr.trim()); }
        catch (DateTimeParseException e) {
            showFormError("Invalid time format. Use HH:MM (e.g. 08:00)."); return;
        }

        int trainerId = SessionManager.getInstance().getCurrentTrainer().getTrainerId();
        saveSessionBtn.setDisable(true);
        saveSessionBtn.setText("Saving...");

        try {
            Session saved = sessionService.scheduleSession(
                    client.getClientId(), trainerId, date, time, fSessionNotes.getText());
            allSessions.add(0, saved);
            populateStatCards();
            hideFormPanel();
            showAlert(Alert.AlertType.INFORMATION, "Session Scheduled",
                    "Session scheduled for " + client.getName() + ".\n"
                            + "Date: " + date + "  Time: " + time + "\n"
                            + "Session balance remaining: " + (client.getSessionBalance() - 1));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            showFormError(ex.getMessage());
            showAlert(Alert.AlertType.ERROR, "Scheduling Failed", ex.getMessage());
        } catch (Exception ex) {
            String msg = "Unexpected error: " + ex.getMessage();
            showFormError(msg);
            showAlert(Alert.AlertType.ERROR, "Error", msg);
        } finally {
            saveSessionBtn.setDisable(false);
            saveSessionBtn.setText("Save Session");
        }
    }

    @FXML private void handleCancelSessionForm(ActionEvent event) { hideFormPanel(); }

    

    private void showFormPanel()  { sessionFormPanel.setVisible(true);  sessionFormPanel.setManaged(true); }
    private void hideFormPanel()  { sessionFormPanel.setVisible(false); sessionFormPanel.setManaged(false); }
    private void showFormError(String msg) { sessionFormErrorLabel.setText(msg); sessionFormErrorLabel.setVisible(true);  sessionFormErrorLabel.setManaged(true); }
    private void hideFormError()           { sessionFormErrorLabel.setText("");  sessionFormErrorLabel.setVisible(false); sessionFormErrorLabel.setManaged(false); }

    private void populateAvatarLabel() {
        var t = SessionManager.getInstance().getCurrentTrainer();
        if (t != null) avatarLabel.setText(String.valueOf(t.getName().charAt(0)).toUpperCase());
    }

    @FXML private void handleNavDashboard(MouseEvent e)   { navigate("DashboardView.fxml",           "TCH — Dashboard"); }
    @FXML private void handleNavClients(MouseEvent e)     { navigate("ClientManagementView.fxml",    "TCH — Clients"); }
    @FXML private void handleNavWorkouts(MouseEvent e)    { navigate("WorkoutTrackingView.fxml",     "TCH — Workouts"); }
    @FXML private void handleNavMemberships(MouseEvent e) { navigate("MembershipManagementView.fxml","TCH — Memberships"); }
    @FXML private void handleNavPayments(MouseEvent e)   { navigate("Payments.fxml",                  "TCH — Payments"); }
    @FXML private void handleNavTrainers(MouseEvent e)   { navigate("Trainers.fxml",                  "TCH — Trainers"); }
    @FXML private void handleNavReports(MouseEvent e)     { navigate("ReportsView.fxml",             "TCH — Reports"); }

    @FXML private void handleLogout(MouseEvent e) { SessionManager.getInstance().logout(); navigate("LoginView.fxml", "TCH — Login"); }

    private void navigate(String fxml, String title) {
        Stage stage = (Stage) sessionTable.getScene().getWindow();
        ViewLoader.navigateTo(stage, fxml, title);
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type); a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    private Optional<ButtonType> showConfirm(String title, String header, String content) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle(title); a.setHeaderText(header); a.setContentText(content);
        return a.showAndWait();
    }
}
