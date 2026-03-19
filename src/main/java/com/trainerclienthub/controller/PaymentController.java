package com.trainerclienthub.controller;

import com.trainerclienthub.model.Client;
import com.trainerclienthub.model.Membership;
import com.trainerclienthub.model.MembershipPlan;
import com.trainerclienthub.model.Payment;
import com.trainerclienthub.model.PaymentMethod;
import com.trainerclienthub.service.ClientService;
import com.trainerclienthub.service.MembershipService;
import com.trainerclienthub.service.PaymentService;
import com.trainerclienthub.util.SessionManager;
import com.trainerclienthub.util.ViewLoader;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller for {@code PaymentManagementView.fxml}.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Display all payments in a filterable {@link TableView}</li>
 *   <li>Show summary cards: total revenue, monthly revenue, payment count</li>
 *   <li>Allow recording new payments via the slide-in form panel</li>
 *   <li>Filter by client name or date range</li>
 * </ul>
 */
public class PaymentController implements Initializable {

    // ── FXML — top bar ────────────────────────────────────────────────────────
    @FXML private Label avatarLabel;

    // ── FXML — summary cards ──────────────────────────────────────────────────
    @FXML private Label totalRevenueLabel;
    @FXML private Label monthlyRevenueLabel;
    @FXML private Label paymentCountLabel;
    @FXML private Label totalCountLabel;

    // ── FXML — filter bar ─────────────────────────────────────────────────────
    @FXML private TextField searchField;
    @FXML private DatePicker filterFrom;
    @FXML private DatePicker filterTo;
    @FXML private Button    addPaymentBtn;

    // ── FXML — table ──────────────────────────────────────────────────────────
    @FXML private TableView<Payment>               paymentTable;
    @FXML private TableColumn<Payment, String>     colPaymentId;
    @FXML private TableColumn<Payment, String>     colClientName;
    @FXML private TableColumn<Payment, String>     colMembershipPlan;
    @FXML private TableColumn<Payment, String>     colAmount;
    @FXML private TableColumn<Payment, String>     colPaymentDate;
    @FXML private TableColumn<Payment, String>     colPaymentMethod;
    @FXML private TableColumn<Payment, String>     colPaymentStatus;
    @FXML private Label                            paymentTableCountLabel;

    // ── FXML — record-payment slide-in form ───────────────────────────────────
    @FXML private VBox              paymentFormPanel;
    @FXML private ComboBox<Client>  fPayClient;
    @FXML private ComboBox<Membership> fPayMembership;
    @FXML private TextField         fPayAmount;
    @FXML private DatePicker        fPayDate;
    @FXML private ComboBox<String>  fPayMethod;
    @FXML private Label             payFormErrorLabel;
    @FXML private Button            savePaymentBtn;

    // ── Services ──────────────────────────────────────────────────────────────
    private final PaymentService    paymentService    = new PaymentService();
    private final ClientService     clientService     = new ClientService();
    private final MembershipService membershipService = new MembershipService();

    // ── State ─────────────────────────────────────────────────────────────────
    private ObservableList<Payment> allPayments;
    private FilteredList<Payment>   filteredPayments;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy");

    // ── Initialise ────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        populateAvatarLabel();
        configureTableColumns();
        loadPayments();
        populateSummaryCards();
        hideFormPanel();
        hideFormError();
    }

    // ── Table setup ───────────────────────────────────────────────────────────

    private void configureTableColumns() {
        // Payment ID
        colPaymentId.setCellValueFactory(data ->
                new SimpleStringProperty(String.valueOf(data.getValue().getPaymentId())));

        // Client name — resolve clientId → name
        colClientName.setCellValueFactory(data -> {
            int cid = data.getValue().getClientId();
            return clientService.findById(cid)
                    .map(c -> new SimpleStringProperty(c.getName()))
                    .orElse(new SimpleStringProperty("Client #" + cid));
        });

        // Membership plan name — resolve membershipId → planId → planName
        colMembershipPlan.setCellValueFactory(data -> {
            int mid = data.getValue().getMembershipId();
            Optional<Membership> mem = membershipService.findById(mid);
            if (mem.isEmpty()) return new SimpleStringProperty("—");
            return membershipService.findPlanById(mem.get().getPlanId())
                    .map(p -> new SimpleStringProperty(p.getPlanName()))
                    .orElse(new SimpleStringProperty("Plan #" + mem.get().getPlanId()));
        });

        // Amount formatted

        colAmount.setCellValueFactory(data ->
                new SimpleStringProperty("Rs " + data.getValue().getAmount()
                        .setScale(2, RoundingMode.HALF_UP).toPlainString()));

        // Date
        colPaymentDate.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getPaymentDate().format(DATE_FMT)));

        // Method
        colPaymentMethod.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getPaymentMethod().name()));

        // Status with colour coding
        colPaymentStatus.setCellValueFactory(data -> {
            var status = data.getValue().getPaymentStatus();
            String label = status == null ? "UNKNOWN" : status.name();
            return new SimpleStringProperty(label);
        });
        colPaymentStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
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

        paymentTable.setItems(filteredPayments == null
                ? FXCollections.observableArrayList()
                : filteredPayments);
    }

    private void loadPayments() {
        List<Payment> payments = paymentService.findAll();
        allPayments     = FXCollections.observableArrayList(payments);
        filteredPayments = new FilteredList<>(allPayments, p -> true);
        paymentTable.setItems(filteredPayments);
        updateTableCountLabel();
    }

    private void updateTableCountLabel() {
        if (paymentTableCountLabel != null) {
            paymentTableCountLabel.setText(filteredPayments.size() + " payment(s)");
        }
    }

    // ── Summary cards ─────────────────────────────────────────────────────────

    private void populateSummaryCards() {
        // Total all-time revenue
        BigDecimal total = paymentService.findAll().stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalRevenueLabel != null)
            totalRevenueLabel.setText("Rs " + formatAmount(total));

        // This month's revenue
        YearMonth current = YearMonth.now();
        BigDecimal monthly = paymentService
                .findByDateRange(current.atDay(1), current.atEndOfMonth())
                .stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (monthlyRevenueLabel != null)
            monthlyRevenueLabel.setText("Rs " + formatAmount(monthly));

        // Total payment count
        int count = allPayments == null ? 0 : allPayments.size();
        if (totalCountLabel  != null) totalCountLabel.setText(String.valueOf(count));
        if (paymentCountLabel != null) paymentCountLabel.setText(count + " this month: " +
                paymentService.findByDateRange(current.atDay(1), current.atEndOfMonth()).size());
    }

    // ── Filter handlers ───────────────────────────────────────────────────────

    @FXML
    private void handleFilter(ActionEvent event) {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        LocalDate from = filterFrom.getValue();
        LocalDate to   = filterTo.getValue();

        filteredPayments.setPredicate(p -> {
            // Client name filter
            if (!keyword.isEmpty()) {
                boolean nameMatch = clientService.findById(p.getClientId())
                        .map(c -> c.getName().toLowerCase().contains(keyword))
                        .orElse(false);
                if (!nameMatch) return false;
            }
            // Date range filter
            if (from != null && p.getPaymentDate().isBefore(from)) return false;
            if (to   != null && p.getPaymentDate().isAfter(to))    return false;
            return true;
        });
        updateTableCountLabel();
    }

    @FXML
    private void handleClearFilter(ActionEvent event) {
        searchField.clear();
        filterFrom.setValue(null);
        filterTo.setValue(null);
        filteredPayments.setPredicate(p -> true);
        updateTableCountLabel();
    }

    // ── Form handlers ─────────────────────────────────────────────────────────

    @FXML
    private void handleAddPayment(ActionEvent event) {
        loadFormDropdowns();
        fPayDate.setValue(LocalDate.now());
        fPayAmount.clear();
        hideFormError();
        showFormPanel();
    }

    @FXML
    private void handleMembershipSelected(ActionEvent event) {
        Membership selected = fPayMembership.getValue();
        if (selected == null) return;
        // Pre-fill the amount from the plan price
        membershipService.findPlanById(selected.getPlanId()).ifPresent(plan -> {
            if (plan.getPrice() != null && plan.getPrice().compareTo(BigDecimal.ZERO) > 0) {
                fPayAmount.setText(plan.getPrice().toPlainString());
            }
        });
    }

    @FXML
    private void handleSavePayment(ActionEvent event) {
        hideFormError();

        Client     client     = fPayClient.getValue();
        Membership membership = fPayMembership.getValue();
        String     amountStr  = fPayAmount.getText();
        LocalDate  date       = fPayDate.getValue();
        String     methodStr  = fPayMethod.getValue();

        if (client     == null) { showFormError("Please select a client.");     return; }
        if (membership == null) { showFormError("Please select a membership."); return; }
        if (amountStr  == null || amountStr.isBlank()) { showFormError("Amount is required."); return; }
        if (date       == null) { showFormError("Please select a payment date."); return; }
        if (methodStr  == null) { showFormError("Please select a payment method."); return; }

        BigDecimal amount;
        try {
            amount = new BigDecimal(amountStr.trim());
        } catch (NumberFormatException e) {
            showFormError("Amount must be a valid number (e.g. 5000.00).");
            return;
        }

        PaymentMethod method;
        try {
            method = PaymentMethod.valueOf(methodStr);
        } catch (IllegalArgumentException e) {
            showFormError("Invalid payment method selected.");
            return;
        }

        savePaymentBtn.setDisable(true);
        savePaymentBtn.setText("Saving...");

        try {
            Payment saved = paymentService.recordPayment(
                    client.getClientId(),
                    membership.getMembershipId(),
                    amount,
                    date,
                    method);

            allPayments.add(0, saved);
            updateTableCountLabel();
            populateSummaryCards();
            hideFormPanel();

            showAlert(Alert.AlertType.INFORMATION, "Payment Recorded",
                    "Payment of Rs " + amount.toPlainString()
                    + " recorded for " + client.getName() + " via " + method + ".");

        } catch (IllegalArgumentException | IllegalStateException ex) {
            showFormError(ex.getMessage());
            showAlert(Alert.AlertType.ERROR, "Save Failed", ex.getMessage());
        } catch (Exception ex) {
            String msg = "Unexpected error: " + ex.getMessage();
            showFormError(msg);
            showAlert(Alert.AlertType.ERROR, "Error", msg);
        } finally {
            savePaymentBtn.setDisable(false);
            savePaymentBtn.setText("Save Payment");
        }
    }

    @FXML
    private void handleCancelPayForm(ActionEvent event) {
        hideFormPanel();
    }

    // ── Form helpers ──────────────────────────────────────────────────────────

    private void loadFormDropdowns() {
        // Clients ComboBox
        fPayClient.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Client c, boolean empty) {
                super.updateItem(c, empty);
                setText(empty || c == null ? null : c.getName());
            }
        });
        fPayClient.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Client c, boolean empty) {
                super.updateItem(c, empty);
                setText(empty || c == null ? "Select client..." : c.getName());
            }
        });
        fPayClient.setItems(FXCollections.observableArrayList(clientService.findAll()));

        // Memberships ComboBox — loads all memberships
        fPayMembership.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Membership m, boolean empty) {
                super.updateItem(m, empty);
                if (empty || m == null) { setText(null); return; }
                String planName = membershipService.findPlanById(m.getPlanId())
                        .map(MembershipPlan::getPlanName)
                        .orElse("Plan #" + m.getPlanId());
                setText("ID " + m.getMembershipId() + " — " + planName
                        + " (" + m.getStatus() + ")");
            }
        });
        fPayMembership.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Membership m, boolean empty) {
                super.updateItem(m, empty);
                setText(empty || m == null ? "Select membership..." :
                        "Membership #" + m.getMembershipId());
            }
        });
        fPayMembership.setItems(FXCollections.observableArrayList(
                membershipService.findAll()));

        // Payment method ComboBox
        fPayMethod.getItems().setAll("CASH", "CARD", "ONLINE", "BANK_TRANSFER");
        fPayMethod.setValue("CASH");
    }

    private void showFormPanel()  { paymentFormPanel.setVisible(true);  paymentFormPanel.setManaged(true); }
    private void hideFormPanel()  { paymentFormPanel.setVisible(false); paymentFormPanel.setManaged(false); }
    private void showFormError(String msg) { payFormErrorLabel.setText(msg); payFormErrorLabel.setVisible(true);  payFormErrorLabel.setManaged(true); }
    private void hideFormError()           { payFormErrorLabel.setText(""); payFormErrorLabel.setVisible(false); payFormErrorLabel.setManaged(false); }

    // ── Sidebar navigation ────────────────────────────────────────────────────

    @FXML private void handleNavDashboard(MouseEvent e)   { navigate("DashboardView.fxml",            "TCH — Dashboard"); }
    @FXML private void handleNavClients(MouseEvent e)     { navigate("ClientManagementView.fxml",     "TCH — Clients"); }
    @FXML private void handleNavWorkouts(MouseEvent e)    { navigate("WorkoutTrackingView.fxml",      "TCH — Workouts"); }
    @FXML private void handleNavMemberships(MouseEvent e) { navigate("MembershipManagementView.fxml", "TCH — Memberships"); }
    @FXML private void handleNavSessions(MouseEvent e)    { navigate("SessionManagementView.fxml",    "TCH — Sessions"); }
    @FXML private void handleNavReports(MouseEvent e)     { navigate("ReportsView.fxml",              "TCH — Reports"); }

    @FXML
    private void handleLogout(MouseEvent e) {
        SessionManager.getInstance().logout();
        navigate("LoginView.fxml", "TCH — Login");
    }

    // ── Private utilities ─────────────────────────────────────────────────────

    private void populateAvatarLabel() {
        var trainer = SessionManager.getInstance().getCurrentTrainer();
        if (trainer != null && !trainer.getName().isBlank()) {
            avatarLabel.setText(String.valueOf(trainer.getName().charAt(0)).toUpperCase());
        }
    }

    private void navigate(String fxml, String title) {
        Stage stage = (Stage) paymentTable.getScene().getWindow();
        ViewLoader.navigateTo(stage, fxml, title);
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) return "0";
        return String.format("%,.2f", amount.setScale(2, RoundingMode.HALF_UP));
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }
}
