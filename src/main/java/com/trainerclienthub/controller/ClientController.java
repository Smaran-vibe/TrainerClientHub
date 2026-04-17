package com.trainerclienthub.controller;

import com.trainerclienthub.model.Client;
import com.trainerclienthub.model.Gender;
import com.trainerclienthub.model.TrainerRole;
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
import javafx.scene.layout.HBox;
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

    @FXML
    private Label avatarLabel;
    @FXML
    private HBox navMemberships;
    @FXML
    private HBox navPayments;
    @FXML
    private HBox navTrainers;

    @FXML
    private TextField searchField;
    @FXML
    private Button addClientBtn;
    @FXML
    private Button editClientBtn;
    @FXML
    private Button deleteClientBtn;

    @FXML
    private TableView<Client> clientTable;
    @FXML
    private TableColumn<Client, Integer> colId;
    @FXML
    private TableColumn<Client, String> colName;
    @FXML
    private TableColumn<Client, Integer> colAge;
    @FXML
    private TableColumn<Client, String> colGender;
    @FXML
    private TableColumn<Client, String> colPhone;
    @FXML
    private TableColumn<Client, String> colEmail;
    @FXML
    private TableColumn<Client, BigDecimal> colWeight;
    @FXML
    private TableColumn<Client, Integer> colSessionBalance;
    @FXML
    private TableColumn<Client, String> colStatus;
    @FXML
    private Label clientCountLabel;

    @FXML
    private VBox formPanel;
    @FXML
    private Label formTitle;
    @FXML
    private TextField fName;
    @FXML
    private TextField fAge;
    @FXML
    private ComboBox<String> fGender;
    @FXML
    private TextField fWeight;
    @FXML
    private TextField fPhone;
    @FXML
    private TextField fEmail;
    @FXML
    private TextField fSessionBalance;
    @FXML
    private Label formErrorLabel;
    @FXML
    private Button formSaveBtn;

    private final ClientService clientService = new ClientService();
    private ObservableList<Client> allClients;
    private FilteredList<Client> filteredClients;

    private boolean editMode = false;

    private Client selectedClientForEdit = null;

    @Override
    // Setup table/form and load initial client list
    public void initialize(URL location, ResourceBundle resources) {
        populateAvatarLabel();
        applyRoleBasedUI();
        configureTableColumns();
        initFormControls();
        loadClients();
        wireSearchFilter();
        hideFormPanel();
        hideFormError();
    }

    // Trainers see a limited sidebar compared to admins
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
    }

    private void initFormControls() {

        fGender.getItems().setAll("Male", "Female", "Other");

        UnaryOperator<TextFormatter.Change> ageFilter = change -> {
            String newText = change.getControlNewText();

            if (newText.isEmpty() || newText.matches("\\d{1,3}")) {
                return change;
            }
            return null;
        };
        fAge.setTextFormatter(new TextFormatter<>(ageFilter));
    }

    private void configureTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("clientId"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colAge.setCellValueFactory(new PropertyValueFactory<>("age"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colWeight.setCellValueFactory(new PropertyValueFactory<>("weightKg"));
        colSessionBalance.setCellValueFactory(new PropertyValueFactory<>("sessionBalance"));

        colGender.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getGender() != null
                        ? capitalize(data.getValue().getGender().name())
                        : ""));

        colStatus.setCellValueFactory(data -> {
            int balance = data.getValue().getSessionBalance();
            String status = balance > 0 ? "Active" : "No Sessions";
            return new javafx.beans.property.SimpleStringProperty(status);
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
                setStyle("Active".equals(item)
                        ? "-fx-text-fill: #00CC66; -fx-font-weight: bold;"
                        : "-fx-text-fill: #FF4444; -fx-font-weight: bold;");
            }
        });
    }

    // Load client rows from the database
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
                if (lower.isEmpty())
                    return true;
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
    // Delete the selected client (also removes related records)
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

    private void deleteClientCascade(int clientId) {

        com.trainerclienthub.DAO.PaymentDAO paymentDAO = new com.trainerclienthub.DAO.PaymentDAO();
        com.trainerclienthub.DAO.MembershipDAO membershipDAO = new com.trainerclienthub.DAO.MembershipDAO();
        com.trainerclienthub.DAO.SessionDAO sessionDAO = new com.trainerclienthub.DAO.SessionDAO();
        com.trainerclienthub.DAO.WorkoutDAO workoutDAO = new com.trainerclienthub.DAO.WorkoutDAO();

        paymentDAO.findByClient(clientId)
                .forEach(p -> paymentDAO.delete(p.getPaymentId()));

        membershipDAO.findByClient(clientId)
                .forEach(m -> membershipDAO.delete(m.getMembershipId()));

        sessionDAO.findByClient(clientId)
                .forEach(s -> sessionDAO.delete(s.getSessionId()));

        workoutDAO.findByClient(clientId)
                .forEach(w -> workoutDAO.delete(w.getWorkoutId()));

        clientService.deleteClient(clientId);
    }

    @FXML
    // Save form changes (add or edit)
    private void handleSaveClient(ActionEvent event) {
        hideFormError();

        String name = fName.getText();
        String ageStr = fAge.getText();
        String genderStr = fGender.getValue();
        String weight = fWeight.getText();
        String phone = fPhone.getText();
        String email = fEmail.getText();
        String balStr = fSessionBalance.getText();

        if (name == null || name.isBlank()) {
            showFormError("Name is required.");
            return;
        }
        if (ageStr == null || ageStr.isBlank()) {
            showFormError("Age is required.");
            return;
        }
        if (genderStr == null) {
            showFormError("Gender is required.");
            return;
        }
        if (weight == null || weight.isBlank()) {
            showFormError("Weight is required.");
            return;
        }
        if (phone == null || phone.isBlank()) {
            showFormError("Phone is required.");
            return;
        }
        if (email == null || email.isBlank()) {
            showFormError("Email is required.");
            return;
        }

        int age;
        try {
            age = Integer.parseInt(ageStr.trim());
        } catch (NumberFormatException e) {
            showFormError("Age must be a whole number.");
            return;
        }

        if (age < 10 || age > 100) {
            showFormError("Age must be between 10 and 100.");
            fAge.requestFocus();
            return;
        }

        BigDecimal weightKg;
        try {
            weightKg = new BigDecimal(weight.trim());
        } catch (NumberFormatException e) {
            showFormError("Weight must be a number (e.g. 72.50).");
            return;
        }

        int balance = 0;
        if (balStr != null && !balStr.isBlank()) {
            try {
                balance = Integer.parseInt(balStr.trim());
            } catch (NumberFormatException e) {
                showFormError("Session balance must be a whole number.");
                return;
            }
        }

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
                if (idx >= 0)
                    allClients.set(idx, selectedClientForEdit);
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
        fName.clear();
        fAge.clear();
        fGender.setValue(null);
        fWeight.clear();
        fPhone.clear();
        fEmail.clear();
        fSessionBalance.clear();
        hideFormError();
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

    @FXML
    private void handleNavDashboard(MouseEvent e) {
        navigate("DashboardView.fxml", "TCH — Dashboard");
    }

    @FXML
    private void handleNavWorkouts(MouseEvent e) {
        navigate("WorkoutTrackingView.fxml", "TCH — Workouts");
    }

    @FXML
    private void handleNavMemberships(MouseEvent e) {
        navigate("MembershipManagementView.fxml", "TCH — Memberships");
    }

    @FXML
    private void handleNavSessions(MouseEvent e) {
        navigate("SessionManagementView.fxml", "TCH — Sessions");
    }

    @FXML
    private void handleNavPayments(MouseEvent e) {
        navigate("Payments.fxml", "TCH — Payments");
    }

    @FXML
    private void handleNavTrainers(MouseEvent e) {
        navigate("Trainers.fxml", "TCH — Trainers");
    }

    @FXML
    private void handleNavReports(MouseEvent e) {
        navigate("ReportsView.fxml", "TCH — Reports");
    }

    @FXML
    private void handleLogout(MouseEvent e) {
        SessionManager.getInstance().logout();
        navigate("LoginView.fxml", "TCH — Login");
    }

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
        if (s == null || s.isEmpty())
            return s;
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
