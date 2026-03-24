package com.trainerclienthub.controller;

import com.trainerclienthub.model.Payment;
import com.trainerclienthub.model.PaymentStatus;
import com.trainerclienthub.model.TrainerRole;
import com.trainerclienthub.service.PaymentService;
import com.trainerclienthub.util.SessionManager;
import com.trainerclienthub.util.ViewLoader;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.util.Optional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Simplified payments dashboard that exposes a fullscreen table of payment records.
 */
public class PaymentController implements Initializable {

    @FXML private Label avatarLabel;
    @FXML private HBox navMemberships;
    @FXML private HBox navPayments;
    @FXML private HBox navTrainers;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilterCombo;
    @FXML private TableView<Payment> paymentTable;
    @FXML private TableColumn<Payment, String> colPaymentId;
    @FXML private TableColumn<Payment, String> colClientName;
    @FXML private TableColumn<Payment, String> colAmount;
    @FXML private TableColumn<Payment, String> colPaymentDate;
    @FXML private TableColumn<Payment, String> colMethod;
    @FXML private TableColumn<Payment, String> colStatus;
    @FXML private TableColumn<Payment, Void> colAction;

    private final PaymentService paymentService = new PaymentService();
    private ObservableList<Payment> payments = FXCollections.observableArrayList();
    private FilteredList<Payment> filteredPayments;
    private SortedList<Payment> sortedPayments;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        applyRoleBasedUI();
        configureTable();
        setupSearchAndFilter();
        loadPayments();
        populateAvatarLabel();
    }

    private void applyRoleBasedUI() {
        boolean isTrainer = SessionManager.getInstance().getRole() == TrainerRole.TRAINER;
        if (navMemberships != null) { navMemberships.setVisible(!isTrainer); navMemberships.setManaged(!isTrainer); }
        if (navPayments != null)    { navPayments.setVisible(!isTrainer);    navPayments.setManaged(!isTrainer); }
        if (navTrainers != null)    { navTrainers.setVisible(!isTrainer);    navTrainers.setManaged(!isTrainer); }
    }

    private void setupSearchAndFilter() {
        statusFilterCombo.setItems(FXCollections.observableArrayList("All", "COMPLETED", "PENDING", "REFUNDED", "FAILED"));
        statusFilterCombo.setValue("All");

        filteredPayments = new FilteredList<>(payments, p -> true);
        sortedPayments = new SortedList<>(filteredPayments, Comparator.comparing(p -> p.getPaymentDate() != null ? p.getPaymentDate() : java.time.LocalDate.MIN, Comparator.reverseOrder()));
        paymentTable.setItems(sortedPayments);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilter());
        statusFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilter());
    }

    private void applyFilter() {
        String search = searchField.getText();
        String status = statusFilterCombo.getValue();
        filteredPayments.setPredicate(p -> {
            String clientName = p.getClientName() != null ? p.getClientName() : "";
            boolean nameMatch = clientName.toLowerCase().contains((search != null ? search : "").toLowerCase().trim());
            boolean statusMatch = status == null || "All".equals(status) || status.equals(p.getPaymentStatus() != null ? p.getPaymentStatus().name() : "");
            return nameMatch && statusMatch;
        });
    }

    private void configureTable() {
        colPaymentId.setCellValueFactory(row ->
                new SimpleStringProperty(String.valueOf(row.getValue().getPaymentId())));

        colClientName.setCellValueFactory(row -> {
            String name = row.getValue().getClientName();
            if (name == null || name.isBlank()) {
                return new SimpleStringProperty("Client #" + row.getValue().getClientId());
            }
            return new SimpleStringProperty(name);
        });

        colAmount.setCellValueFactory(row ->
                new SimpleStringProperty("Rs " + formatAmount(row.getValue().getAmount())));

        colPaymentDate.setCellValueFactory(row ->
                new SimpleStringProperty(row.getValue().getPaymentDate().format(DATE_FMT)));

        colMethod.setCellValueFactory(row ->
                new SimpleStringProperty(row.getValue().getPaymentMethod().name()));

        colStatus.setCellValueFactory(row -> {
            var status = row.getValue().getPaymentStatus();
            return new SimpleStringProperty(status == null ? "UNKNOWN" : status.name());
        });

        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                setStyle(switch (item) {
                    case "COMPLETED" -> "-fx-text-fill:#CCFF00; -fx-font-weight:bold;";
                    case "PENDING"   -> "-fx-text-fill:#FFAA00; -fx-font-weight:bold;";
                    case "REFUNDED"  -> "-fx-text-fill:#AAAAAA;";
                    case "FAILED"    -> "-fx-text-fill:#FF4444;";
                    default          -> "";
                });
            }
        });

        colAction.setCellValueFactory(param -> null);
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");

            {
                editBtn.getStyleClass().add("btn-primary");
                editBtn.setOnAction(event -> {
                    Payment payment = getTableRow().getItem();
                    if (payment != null) {
                        handleEditStatus(payment);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    setGraphic(editBtn);
                }
            }
        });

        paymentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        paymentTable.setPlaceholder(new Label("No payments available."));
    }

    private void loadPayments() {
        List<Payment> loaded = paymentService.findAll();
        payments.setAll(loaded);
        applyFilter();
    }

    private void handleEditStatus(Payment payment) {
        ChoiceDialog<String> dialog = new ChoiceDialog<>(
                payment.getPaymentStatus() != null ? payment.getPaymentStatus().name() : "PENDING",
                "COMPLETED", "PENDING", "REFUNDED", "FAILED"
        );
        dialog.setTitle("Edit Payment Status");
        dialog.setHeaderText("Payment #" + payment.getPaymentId());
        dialog.setContentText("Select new status:");

        java.net.URL cssUrl = getClass().getResource("/com/trainerclienthub/css/neon-theme.css");
        if (cssUrl != null) {
            dialog.getDialogPane().getStylesheets().add(cssUrl.toExternalForm());
        }

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newStatus -> {
            try {
                paymentService.updatePaymentStatus(payment.getPaymentId(), newStatus);
                payment.setPaymentStatus(PaymentStatus.valueOf(newStatus));
                paymentTable.refresh();
            } catch (Exception ex) {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                alert.setTitle("Update Failed");
                alert.setHeaderText("Could not update payment status");
                alert.setContentText(ex.getMessage());
                alert.showAndWait();
            }
        });
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) return "0.00";
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private void populateAvatarLabel() {
        var trainer = SessionManager.getInstance().getCurrentTrainer();
        if (trainer != null && !trainer.getName().isBlank()) {
            avatarLabel.setText(String.valueOf(trainer.getName().charAt(0)).toUpperCase());
        }
    }

    // Sidebar navigation

    @FXML private void handleNavDashboard(MouseEvent event) {
        navigate("DashboardView.fxml", "TCH — Dashboard");
    }

    @FXML private void handleNavClients(MouseEvent event) {
        navigate("ClientManagementView.fxml", "TCH — Clients");
    }

    @FXML private void handleNavWorkouts(MouseEvent event) {
        navigate("WorkoutTrackingView.fxml", "TCH — Workouts");
    }

    @FXML private void handleNavMemberships(MouseEvent event) {
        navigate("MembershipManagementView.fxml", "TCH — Memberships");
    }

    @FXML private void handleNavSessions(MouseEvent event) {
        navigate("SessionManagementView.fxml", "TCH — Sessions");
    }

    @FXML private void handleNavPayments(MouseEvent event) {
        navigate("Payments.fxml", "TCH — Payments");
    }

    @FXML private void handleNavTrainers(MouseEvent event) {
        navigate("Trainers.fxml", "TCH â€” Trainers");
    }

    @FXML private void handleNavReports(MouseEvent event) {
        navigate("ReportsView.fxml", "TCH — Reports");
    }

    @FXML
    private void handleLogout(MouseEvent event) {
        SessionManager.getInstance().logout();
        Stage stage = (Stage) paymentTable.getScene().getWindow();
        ViewLoader.navigateTo(stage, "LoginView.fxml", "TCH — Login");
    }

    private void navigate(String fxml, String title) {
        Stage stage = (Stage) paymentTable.getScene().getWindow();
        ViewLoader.navigateTo(stage, fxml, title);
    }
}
