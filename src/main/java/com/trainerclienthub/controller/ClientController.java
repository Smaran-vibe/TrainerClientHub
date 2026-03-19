package com.trainerclienthub.controller;

import com.trainerclienthub.model.Client;
import com.trainerclienthub.model.Gender;
import com.trainerclienthub.service.ClientService;
import com.trainerclienthub.util.SessionManager;
import com.trainerclienthub.util.ViewLoader;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.UnaryOperator;

public class ClientController implements Initializable {

    //  FXML — top bar
    @FXML private Label avatarLabel;

    //  FXML — action bar
    @FXML private TextField searchField;
    @FXML private Button    addClientBtn;
    @FXML private Button    editClientBtn;
    @FXML private Button    deleteClientBtn;

    // FXML — table
    @FXML private TableView<Client>             clientTable;
    @FXML private TableColumn<Client, Integer>  colId;
    @FXML private TableColumn<Client, String>   colName;
    @FXML private TableColumn<Client, Integer>  colAge;
    @FXML private TableColumn<Client, String>   colGender;
    @FXML private TableColumn<Client, String>   colPhone;
    @FXML private TableColumn<Client, String>   colEmail;
    @FXML private TableColumn<Client, BigDecimal> colWeight;
    @FXML private TableColumn<Client, Integer>  colSessionBalance;
    @FXML private TableColumn<Client, String>   colStatus;
    @FXML private Label                         clientCountLabel;

    //  FXML — slide-in form
    @FXML private VBox      formPanel;
    @FXML private Label     formTitle;
    @FXML private TextField fName;
    @FXML private TextField fAge;
    @FXML private ComboBox<String> fGender;
    @FXML private TextField fWeight;
    @FXML private TextField fPhone;
    @FXML private TextField fEmail;
    @FXML private TextField fSessionBalance;
    @FXML private Label     formErrorLabel;
    @FXML private Button    formSaveBtn;

    // State
    private final ClientService clientService = new ClientService();
    private ObservableList<Client> allClients;
    private FilteredList<Client>   filteredClients;

    /** Tracks whether the form is in ADD or EDIT mode. */
    private boolean editMode = false;
    /** The client currently being edited (null when adding). */
    private Client  selectedClientForEdit = null;

    //  Initialise

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        populateAvatarLabel();
        configureTableColumns();
        initFormControls();
        loadClients();
        wireSearchFilter();
        hideFormPanel();
        hideFormError();
    }

    //  Form control setup

    /**
     * Configures the Gender ComboBox items and restricts the Age field to
     * numeric-only input (digits 0-9, max 3 characters). Called once from
     */
    private void initFormControls() {

        fGender.getItems().setAll("Male", "Female", "Other");

        // Age TextFormatter — accepts only digit characters, maximum 3 characters
        // (100 is the maximum allowed age). Any non-digit keystroke is silently
        UnaryOperator<TextFormatter.Change> ageFilter = change -> {
            String newText = change.getControlNewText();

            if (newText.isEmpty() || newText.matches("\\d{1,3}")) {
                return change;
            }
            return null;  // reject the change — field stays unchanged
        };
        fAge.setTextFormatter(new TextFormatter<>(ageFilter));
    }

    //  Table setup

    private void configureTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("clientId"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colAge.setCellValueFactory(new PropertyValueFactory<>("age"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colWeight.setCellValueFactory(new PropertyValueFactory<>("weightKg"));
        colSessionBalance.setCellValueFactory(new PropertyValueFactory<>("sessionBalance"));

        // Gender enum → display string
        colGender.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getGender() != null
                                ? capitalize(data.getValue().getGender().name()) : ""));

        // Status column derived from session balance
        colStatus.setCellValueFactory(data -> {
            int balance = data.getValue().getSessionBalance();
            String status = balance > 0 ? "Active" : "No Sessions";
            return new javafx.beans.property.SimpleStringProperty(status);
        });

        // Colour the status cell
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle("Active".equals(item)
                        ? "-fx-text-fill: #00CC66; -fx-font-weight: bold;"
                        : "-fx-text-fill: #FF4444; -fx-font-weight: bold;");
            }
        });
    }

    private void loadClients() {
        List<Client> clients = clientService.findAll();
        allClients = FXCollections.observableArrayList(clients);
        filteredClients = new FilteredList<>(allClients, c -> true);
        clientTable.setItems(filteredClients);
        updateCountLabel();
    }

    private void wireSearchFilter() {
        searchField.textProperty().addListener((obs, oldVal, keyword) -> {
            String lower = keyword == null ? "" : keyword.trim().toLowerCase();
            filteredClients.setPredicate(client -> {
                if (lower.isEmpty()) return true;
                return client.getName().toLowerCase().contains(lower)
                        || client.getEmail().toLowerCase().contains(lower)
                        || client.getPhone().contains(lower);
            });
            updateCountLabel();
        });
    }

    private void updateCountLabel() {
        int shown = filteredClients.size();
        int total = allClients.size();
        clientCountLabel.setText(shown == total
                ? total + " clients"
                : shown + " of " + total + " clients");
    }

    //  Action bar handlers

    @FXML
    private void handleAddClient(ActionEvent event) {
        editMode = false;
        selectedClientForEdit = null;
        formTitle.setText("Add New Client");
        clearForm();
        showFormPanel();
    }

    @FXML
    private void handleEditClient(ActionEvent event) {
        Client selected = clientTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No selection", "Please select a client to edit.");
            return;
        }
        editMode = true;
        selectedClientForEdit = selected;
        formTitle.setText("Edit Client");
        populateForm(selected);
        showFormPanel();
    }

    @FXML
    private void handleDeleteClient(ActionEvent event) {
        Client selected = clientTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No selection",
                    "Please select a client to delete.");
            return;
        }

        Optional<ButtonType> result = showConfirm(
                "Delete Client",
                "Delete \"" + selected.getName() + "\"?",
                "All sessions, workouts, memberships, and payments for this client "
                        + "will also be permanently deleted. This cannot be undone.");

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Delete all child records first to satisfy FK constraints
                // (schema uses ON DELETE RESTRICT on all client FK references)
                deleteClientCascade(selected.getClientId());
                allClients.remove(selected);
                updateCountLabel();
                hideFormPanel();
                showAlert(Alert.AlertType.INFORMATION, "Deleted",
                        "Client \"" + selected.getName() + "\" was deleted successfully.");
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Delete Failed", ex.getMessage());
            }
        }
    }

    /**
     * Deletes all child records owned by a client before deleting the client row.
     */
    private void deleteClientCascade(int clientId) {
        // Use each DAO directly for the delete cascade — no service needed,

        com.trainerclienthub.DAO.PaymentDAO paymentDAO =
                new com.trainerclienthub.DAO.PaymentDAO();
        com.trainerclienthub.DAO.MembershipDAO membershipDAO =
                new com.trainerclienthub.DAO.MembershipDAO();
        com.trainerclienthub.DAO.SessionDAO sessionDAO =
                new com.trainerclienthub.DAO.SessionDAO();
        com.trainerclienthub.DAO.WorkoutDAO workoutDAO =
                new com.trainerclienthub.DAO.WorkoutDAO();

        // 1. Payments reference client + membership — delete first
        paymentDAO.findByClient(clientId)
                .forEach(p -> paymentDAO.delete(p.getPaymentId()));

        // 2. Memberships reference client
        membershipDAO.findByClient(clientId)
                .forEach(m -> membershipDAO.delete(m.getMembershipId()));

        // 3. Sessions reference client
        sessionDAO.findByClient(clientId)
                .forEach(s -> sessionDAO.delete(s.getSessionId()));

        // 4. Workouts — exercises are auto-deleted via ON DELETE CASCADE
        workoutDAO.findByClient(clientId)
                .forEach(w -> workoutDAO.delete(w.getWorkoutId()));

        // 5. Finally delete the client row
        clientService.deleteClient(clientId);
    }

    //  Form handlers
    @FXML
    private void handleSaveClient(ActionEvent event) {
        hideFormError();

        String name    = fName.getText();
        String ageStr  = fAge.getText();
        String genderStr = fGender.getValue();
        String weight  = fWeight.getText();
        String phone   = fPhone.getText();
        String email   = fEmail.getText();
        String balStr  = fSessionBalance.getText();

        // UI-level pre-checks
        if (name == null || name.isBlank())    { showFormError("Name is required."); return; }
        if (ageStr == null || ageStr.isBlank()) { showFormError("Age is required."); return; }
        if (genderStr == null)                  { showFormError("Gender is required."); return; }
        if (weight == null || weight.isBlank()) { showFormError("Weight is required."); return; }
        if (phone == null || phone.isBlank())   { showFormError("Phone is required."); return; }
        if (email == null || email.isBlank())   { showFormError("Email is required."); return; }

        int age;
        try {
            age = Integer.parseInt(ageStr.trim());
        } catch (NumberFormatException e) {
            showFormError("Age must be a whole number.");
            return;
        }

        // Validate age range explicitly here so the user gets a clear message
        if (age < 10 || age > 100) {
            showFormError("Age must be between 10 and 100.");
            fAge.requestFocus();
            return;
        }

        BigDecimal weightKg;
        try { weightKg = new BigDecimal(weight.trim()); }
        catch (NumberFormatException e) { showFormError("Weight must be a number (e.g. 72.50)."); return; }

        int balance = 0;
        if (balStr != null && !balStr.isBlank()) {
            try { balance = Integer.parseInt(balStr.trim()); }
            catch (NumberFormatException e) { showFormError("Session balance must be a whole number."); return; }
        }

        // ComboBox shows "Male" / "Female" / "Other" — convert to enum name
        Gender gender = Gender.valueOf(genderStr.toUpperCase());
        int trainerId = SessionManager.getInstance().getCurrentTrainer().getTrainerId();

        formSaveBtn.setDisable(true);
        formSaveBtn.setText("Saving...");

        try {
            if (editMode && selectedClientForEdit != null) {
                selectedClientForEdit.setName(name);
                selectedClientForEdit.setAge(age);
                selectedClientForEdit.setGender(gender);
                selectedClientForEdit.setPhone(phone);
                selectedClientForEdit.setEmail(email);
                selectedClientForEdit.setSessionBalance(balance);
                selectedClientForEdit.setWeightKg(weightKg);
                clientService.updateClient(selectedClientForEdit);
                int idx = allClients.indexOf(selectedClientForEdit);
                if (idx >= 0) allClients.set(idx, selectedClientForEdit);
                updateCountLabel();
                hideFormPanel();
                clearForm();
                showAlert(Alert.AlertType.INFORMATION, "Updated",
                        "Client \"" + name + "\" updated successfully.");
            } else {
                Client newClient = clientService.addClient(
                        name, age, gender, phone, email, balance, weightKg, trainerId);
                allClients.add(newClient);
                updateCountLabel();
                hideFormPanel();
                clearForm();
                showAlert(Alert.AlertType.INFORMATION, "Client Added",
                        "Client \"" + name + "\" was added successfully.");
            }

        } catch (IllegalArgumentException | IllegalStateException ex) {
            showFormError(ex.getMessage());
            showAlert(Alert.AlertType.ERROR, "Validation Error", ex.getMessage());
        } catch (Exception ex) {
            String msg = "An unexpected error occurred: " + ex.getMessage();
            showFormError(msg);
            showAlert(Alert.AlertType.ERROR, "Error", msg);
        } finally {
            formSaveBtn.setDisable(false);
            formSaveBtn.setText("Save Client");
        }
    }

    @FXML
    private void handleCancelForm(ActionEvent event) {
        hideFormPanel();
        clearForm();
    }

    //  Form helpers
    private void populateForm(Client c) {
        fName.setText(c.getName());
        fAge.setText(String.valueOf(c.getAge()));
        fGender.setValue(capitalize(c.getGender().name()));
        fWeight.setText(c.getWeightKg().toPlainString());
        fPhone.setText(c.getPhone());
        fEmail.setText(c.getEmail());
        fSessionBalance.setText(String.valueOf(c.getSessionBalance()));
    }

    private void clearForm() {
        fName.clear(); fAge.clear(); fGender.setValue(null);
        fWeight.clear(); fPhone.clear(); fEmail.clear();
        fSessionBalance.clear(); hideFormError();
    }

    private void showFormPanel() {
        formPanel.setVisible(true);
        formPanel.setManaged(true);
    }

    private void hideFormPanel() {
        formPanel.setVisible(false);
        formPanel.setManaged(false);
    }

    private void showFormError(String msg) {
        formErrorLabel.setText(msg);
        formErrorLabel.setVisible(true);
        formErrorLabel.setManaged(true);
    }

    private void hideFormError() {
        formErrorLabel.setText("");
        formErrorLabel.setVisible(false);
        formErrorLabel.setManaged(false);
    }

    // Sidebar navigation

    @FXML private void handleNavDashboard(MouseEvent e)   { navigate("DashboardView.fxml",           "TCH — Dashboard"); }
    @FXML private void handleNavWorkouts(MouseEvent e)    { navigate("WorkoutTrackingView.fxml",     "TCH — Workouts"); }
    @FXML private void handleNavMemberships(MouseEvent e) { navigate("MembershipManagementView.fxml","TCH — Memberships"); }
    @FXML private void handleNavSessions(MouseEvent e)    { navigate("SessionManagementView.fxml",   "TCH — Sessions"); }
    @FXML private void handleNavPayments(MouseEvent e)   { navigate("PaymentManagementView.fxml", "TCH — Payments"); }
    @FXML private void handleNavReports(MouseEvent e)     { navigate("ReportsView.fxml",             "TCH — Reports"); }

    @FXML
    private void handleLogout(MouseEvent e) {
        SessionManager.getInstance().logout();
        navigate("LoginView.fxml", "TCH — Login");
    }

    //  Private utilities

    private void populateAvatarLabel() {
        var trainer = SessionManager.getInstance().getCurrentTrainer();
        if (trainer != null && !trainer.getName().isBlank()) {
            avatarLabel.setText(String.valueOf(trainer.getName().charAt(0)).toUpperCase());
        }
    }

    private void navigate(String fxml, String title) {
        Stage stage = (Stage) clientTable.getScene().getWindow();
        ViewLoader.navigateTo(stage, fxml, title);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.charAt(0) + s.substring(1).toLowerCase();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private Optional<ButtonType> showConfirm(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        return alert.showAndWait();
    }
}