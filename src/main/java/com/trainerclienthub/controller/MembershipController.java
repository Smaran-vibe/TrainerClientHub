package com.trainerclienthub.controller;

import com.trainerclienthub.model.Client;
import com.trainerclienthub.model.Membership;
import com.trainerclienthub.model.TrainerRole;
import com.trainerclienthub.model.MembershipPlan;
import com.trainerclienthub.model.MembershipStatus;
import com.trainerclienthub.model.PaymentMethod;
import com.trainerclienthub.model.PlanType;
import com.trainerclienthub.service.ClientService;
import com.trainerclienthub.service.MembershipService;
import com.trainerclienthub.service.PaymentService;
import com.trainerclienthub.util.SessionManager;
import com.trainerclienthub.util.ViewLoader;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Function;


public class MembershipController implements Initializable {


    @FXML
    private Label avatarLabel;
    @FXML
    private HBox navMemberships;
    @FXML
    private HBox navPayments;
    @FXML
    private HBox navTrainers;


    @FXML
    private TabPane membershipTabs;
    @FXML
    private TextField membershipSearchField;
    @FXML
    private ComboBox<String> statusFilter;
    @FXML
    private Button assignMembershipBtn;
    @FXML
    private Button renewBtn;
    @FXML
    private Button cancelMembershipBtn;

    @FXML
    private TableView<Membership> membershipTable;
    @FXML
    private TableColumn<Membership, Integer> colMemId;
    @FXML
    private TableColumn<Membership, String> colMemClient;
    @FXML
    private TableColumn<Membership, String> colMemPlan;
    @FXML
    private TableColumn<Membership, String> colMemType;
    @FXML
    private TableColumn<Membership, LocalDate> colMemStart;
    @FXML
    private TableColumn<Membership, LocalDate> colMemEnd;
    @FXML
    private TableColumn<Membership, String> colMemStatus;
    @FXML
    private TableColumn<Membership, Long> colDaysLeft;


    @FXML
    private Button addPlanBtn;
    @FXML
    private Button editPlanBtn;
    @FXML
    private Button deletePlanBtn;

    @FXML
    private TableView<MembershipPlan> planTable;
    @FXML
    private TableColumn<MembershipPlan, Integer> colPlanId;
    @FXML
    private TableColumn<MembershipPlan, String> colPlanName;
    @FXML
    private TableColumn<MembershipPlan, String> colPlanType;
    @FXML
    private TableColumn<MembershipPlan, Integer> colPlanDuration;
    @FXML
    private TableColumn<MembershipPlan, BigDecimal> colPlanPrice;


    @FXML
    private VBox membershipFormPanel;
    @FXML
    private Label membershipFormTitle;
    @FXML
    private ComboBox<Client> fMemClient;
    @FXML
    private ComboBox<MembershipPlan> fMemPlan;
    @FXML
    private VBox planPreviewBox;
    @FXML
    private Label planPreviewName;
    @FXML
    private Label planPreviewPrice;
    @FXML
    private Label planPreviewDays;
    @FXML
    private DatePicker fMemStartDate;
    @FXML
    private DatePicker fMemEndDate;
    @FXML
    private ComboBox<String> fPaymentMethod;
    @FXML
    private Label memFormErrorLabel;
    @FXML
    private Button saveMemBtn;


    private final MembershipService membershipService = new MembershipService();
    private final ClientService clientService = new ClientService();
    private final PaymentService paymentService = new PaymentService();

    private final ObservableList<Membership> memberships = FXCollections.observableArrayList();
    private FilteredList<Membership> filteredMemberships;
    private final ObservableList<MembershipPlan> plans = FXCollections.observableArrayList();


    @Override
    // Setup tables/forms and load current memberships/plans
    public void initialize(URL location, ResourceBundle resources) {
        populateAvatarLabel();
        applyRoleBasedUI();
        configureMembershipTable();
        configurePlanTable();
        loadMemberships();
        filteredMemberships = new FilteredList<>(memberships);
        SortedList<Membership> sortedMemberships = new SortedList<>(filteredMemberships);
        membershipTable.setItems(sortedMemberships);
        sortedMemberships.comparatorProperty().bind(membershipTable.comparatorProperty());
        loadPlans();
        statusFilter.setItems(FXCollections.observableArrayList(
                MembershipStatus.ACTIVE.name(),
                MembershipStatus.EXPIRED.name(),
                MembershipStatus.CANCELLED.name()));
        statusFilter.setValue(MembershipStatus.ACTIVE.name());
        wireSearchAndFilter();
    }

    // Trainers shouldn't see admin-only navigation
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


    private void configureMembershipTable() {
        colMemId.setCellValueFactory(new PropertyValueFactory<>("membershipId"));
        colMemStart.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        colMemEnd.setCellValueFactory(new PropertyValueFactory<>("endDate"));

        colMemClient.setCellValueFactory(data -> {
            Optional<Client> c = clientService.findById(data.getValue().getClientId());
            return new javafx.beans.property.SimpleStringProperty(
                    c.map(Client::getName).orElse("Unknown"));
        });

        colMemPlan.setCellValueFactory(data -> {
            Optional<MembershipPlan> p = membershipService.findPlanById(data.getValue().getPlanId());
            return new javafx.beans.property.SimpleStringProperty(
                    p.map(MembershipPlan::getPlanName).orElse("Unknown"));
        });

        colMemType.setCellValueFactory(data -> {
            Optional<MembershipPlan> p = membershipService.findPlanById(data.getValue().getPlanId());
            return new javafx.beans.property.SimpleStringProperty(
                    p.map(mp -> mp.getPlanType().name()).orElse(""));
        });

        colMemStatus.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getStatus().name()));

        colMemStatus.setCellFactory(col -> new TableCell<>() {
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
                    case "ACTIVE" -> "-fx-text-fill:#CCFF00; -fx-font-weight:bold;";
                    case "EXPIRED" -> "-fx-text-fill:#FF4444; -fx-font-weight:bold;";
                    case "CANCELLED" -> "-fx-text-fill:#AAAAAA;";
                    default -> "";
                });
            }
        });

        colDaysLeft.setCellValueFactory(data -> {
            LocalDate end = data.getValue().getEndDate();
            long days = ChronoUnit.DAYS.between(LocalDate.now(), end);
            return new javafx.beans.property.SimpleLongProperty(Math.max(0, days)).asObject();
        });

    }

    private void configurePlanTable() {
        colPlanId.setCellValueFactory(new PropertyValueFactory<>("planId"));
        colPlanName.setCellValueFactory(new PropertyValueFactory<>("planName"));
        colPlanDuration.setCellValueFactory(new PropertyValueFactory<>("durationDays"));
        colPlanPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colPlanType.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getPlanType().name()));
        planTable.setItems(plans);
    }

    // Refresh memberships and re-apply the current filter (if any)
    private void loadMemberships() {
        memberships.setAll(membershipService.findAll());
        if (filteredMemberships != null) {
            applyFilter();
        }
    }

    private void loadPlans() {
        plans.setAll(membershipService.findAllPlans());
    }

    private void wireSearchAndFilter() {
        membershipSearchField.textProperty().addListener((obs, o, keyword) -> applyFilter());
        statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilter());

        applyFilter();
    }

    private void applyFilter() {
        if (filteredMemberships == null) return;

        String keyword = membershipSearchField.getText().trim().toLowerCase();
        String status = statusFilter.getValue();

        filteredMemberships.setPredicate(membership -> {
            boolean keywordMatches = keyword.isEmpty() || matchesClientName(membership, keyword);

            boolean statusMatches = status == null || status.isBlank()
                    || membership.getStatus().name().equalsIgnoreCase(status);

            return keywordMatches && statusMatches;
        });
    }

    private boolean matchesClientName(Membership membership, String keyword) {
        Optional<Client> client = clientService.findById(membership.getClientId());
        return client.map(c -> c.getName().toLowerCase().contains(keyword)).orElse(false);
    }


    @FXML
    private void handleAssignMembership(ActionEvent event) {
        membershipFormTitle.setText("Assign Membership");
        loadFormDropdowns();
        fMemStartDate.setValue(LocalDate.now());
        fMemEndDate.setValue(null);
        planPreviewBox.setVisible(false);
        planPreviewBox.setManaged(false);
        hideFormError();
        showFormPanel();
    }

    @FXML
    private void handleRenewMembership(ActionEvent event) {
        Membership selected = membershipTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Select a membership to renew.");
            return;
        }
        if (selected.getStatus() != MembershipStatus.ACTIVE) {
            showAlert(Alert.AlertType.WARNING, "Not Active",
                    "Only ACTIVE memberships can be renewed.");
            return;
        }

        Optional<MembershipPlan> planOpt = membershipService.findPlanById(selected.getPlanId());
        int days = planOpt.map(MembershipPlan::getDurationDays).orElse(30);
        LocalDate newEnd = selected.getEndDate().plusDays(days);

        try {
            membershipService.renewMembership(selected.getMembershipId(), newEnd);


            planOpt.ifPresent(plan -> {
                if (plan.getPrice() != null
                        && plan.getPrice().compareTo(java.math.BigDecimal.ZERO) > 0) {
                    paymentService.recordPayment(
                            selected.getClientId(),
                            selected.getMembershipId(),
                            plan.getPrice(),
                            java.time.LocalDate.now(),
                            PaymentMethod.CASH);
                }
            });

            loadMemberships();
            showAlert(Alert.AlertType.INFORMATION, "Membership Renewed",
                    "Membership renewed successfully until " + newEnd + ".\n"
                            + "Payment recorded.");
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Renewal Failed", ex.getMessage());
        }
    }

    @FXML
    private void handleCancelMembership(ActionEvent event) {
        Membership selected = membershipTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Select a membership to cancel.");
            return;
        }

        Optional<ButtonType> confirm = showConfirm("Cancel Membership",
                "Cancel this membership?", "The record will be kept for history.");
        if (confirm.isPresent() && confirm.get() == ButtonType.OK) {
            try {
                membershipService.cancelMembership(selected.getMembershipId());
                loadMemberships();
                showAlert(Alert.AlertType.INFORMATION, "Membership Cancelled",
                        "Membership cancelled successfully. "
                                + "The record has been kept for history.");
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Failed", ex.getMessage());
            }
        }
    }


    // Open dialog to create a new membership plan
    @FXML
    private void handleAddPlan(ActionEvent event) {
        openPlanDialog(null, "Plan Created", planName -> "Membership plan \"" + planName + "\" created.");
    }

    @FXML
    private void handleEditPlan(ActionEvent event) {
        MembershipPlan selected = planTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Select a plan to edit.");
            return;
        }
        openPlanDialog(selected, "Plan Updated",
                planName -> "Membership plan \"" + planName + "\" updated successfully.");
    }


    @FXML
    private void handleDeletePlan(ActionEvent event) {
        MembershipPlan selected = planTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Select a plan to delete.");
            return;
        }
        Optional<ButtonType> confirm = showConfirm("Delete Plan", "Delete \"" + selected.getPlanName() + "\"?", "");
        if (confirm.isPresent() && confirm.get() == ButtonType.OK) {
            try {
                membershipService.deletePlan(selected.getPlanId());
                loadPlans();
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Failed", ex.getMessage());
            }
        }
    }

    private void openPlanDialog(MembershipPlan plan, String dialogTitle, Function<String, String> messageFactory) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/trainerclienthub/NewPlanDialog.fxml"));
            Parent root = loader.load();
            NewPlanDialogController dialogController = loader.getController();
            dialogController.setMembershipService(membershipService);
            Stage dialogStage = new Stage(StageStyle.UNDECORATED);
            dialogStage.initOwner(membershipTabs.getScene().getWindow());
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogController.setStage(dialogStage);
            if (plan != null) {
                dialogController.setExistingPlan(plan);
            }
            dialogController.setOnPlanCreated(savedPlan -> {
                loadPlans();
                loadFormDropdowns();
                showAlert(Alert.AlertType.INFORMATION, dialogTitle, messageFactory.apply(savedPlan.getPlanName()));
            });

            Scene scene = new Scene(root);
            scene.getStylesheets().add("/com/trainerclienthub/css/neon-theme.css");
            dialogStage.setScene(scene);
            dialogStage.setTitle(dialogTitle);
            dialogStage.showAndWait();
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "Dialog Failed", "Unable to open the Plan form.");
        }
    }


    @FXML
    private void handlePlanSelected(ActionEvent event) {
        MembershipPlan plan = fMemPlan.getValue();
        if (plan == null) return;
        planPreviewName.setText(plan.getPlanName());
        planPreviewPrice.setText("Rs " + plan.getPrice().toPlainString());
        planPreviewDays.setText(plan.getDurationDays() + " days");
        planPreviewBox.setVisible(true);
        planPreviewBox.setManaged(true);
        autoFillEndDate();
    }

    @FXML
    private void handleStartDateSelected(ActionEvent event) {
        autoFillEndDate();
    }

    private void autoFillEndDate() {
        MembershipPlan plan = fMemPlan.getValue();
        LocalDate start = fMemStartDate.getValue();
        if (plan != null && start != null) {
            fMemEndDate.setValue(start.plusDays(plan.getDurationDays()));
        }
    }

    // Create/update a membership from the form fields
    @FXML
    private void handleSaveMembership(ActionEvent event) {
        hideFormError();
        Client client = fMemClient.getValue();
        MembershipPlan plan = fMemPlan.getValue();
        LocalDate start = fMemStartDate.getValue();
        LocalDate end = fMemEndDate.getValue();

        if (client == null) {
            showFormError("Please select a client.");
            return;
        }
        if (plan == null) {
            showFormError("Please select a membership plan.");
            return;
        }
        if (start == null) {
            showFormError("Please select a start date.");
            return;
        }
        if (end == null) {
            showFormError("Please select an end date.");
            return;
        }


        PaymentMethod paymentMethod = PaymentMethod.CASH;
        if (fPaymentMethod != null && fPaymentMethod.getValue() != null) {
            try {
                paymentMethod = PaymentMethod.valueOf(fPaymentMethod.getValue());
            } catch (IllegalArgumentException ignored) {
                paymentMethod = PaymentMethod.CASH;
            }
        }

        try {

            Membership membership = membershipService.assignMembership(
                    client.getClientId(), plan.getPlanId(), start, end);


            if (plan.getPrice() != null
                    && plan.getPrice().compareTo(java.math.BigDecimal.ZERO) > 0) {
                paymentService.recordPayment(
                        client.getClientId(),
                        membership.getMembershipId(),
                        plan.getPrice(),
                        java.time.LocalDate.now(),
                        paymentMethod);
            }

            loadMemberships();
            hideFormPanel();
            showAlert(Alert.AlertType.INFORMATION, "Membership Assigned",
                    "Membership \"" + plan.getPlanName() + "\" assigned to "
                            + client.getName() + " successfully.\n"
                            + "Valid from " + start + " to " + end + ".\n"
                            + "Payment of Rs " + plan.getPrice().toPlainString()
                            + " recorded via " + paymentMethod + ".");

        } catch (IllegalArgumentException | IllegalStateException ex) {
            showFormError(ex.getMessage());
            showAlert(Alert.AlertType.ERROR, "Assignment Failed", ex.getMessage());
        } catch (Exception ex) {
            String msg = "Unexpected error: " + ex.getMessage();
            showFormError(msg);
            showAlert(Alert.AlertType.ERROR, "Error", msg);
        }
    }

    @FXML
    private void handleCancelMemForm(ActionEvent event) {
        hideFormPanel();
    }


    private void loadFormDropdowns() {
        fMemClient.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Client c, boolean empty) {
                super.updateItem(c, empty);
                setText(empty || c == null ? null : c.getName());
            }
        });
        fMemClient.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Client c, boolean empty) {
                super.updateItem(c, empty);
                setText(empty || c == null ? "Select client..." : c.getName());
            }
        });
        fMemClient.setItems(FXCollections.observableArrayList(clientService.findAll()));

        fMemPlan.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(MembershipPlan p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? null : p.getPlanName());
            }
        });
        fMemPlan.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(MembershipPlan p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? "Select plan..." : p.getPlanName());
            }
        });
        fMemPlan.setItems(FXCollections.observableArrayList(membershipService.findAllPlans()));
    }

    private void showFormPanel() {
        membershipFormPanel.setVisible(true);
        membershipFormPanel.setManaged(true);
    }

    private void hideFormPanel() {
        membershipFormPanel.setVisible(false);
        membershipFormPanel.setManaged(false);
    }

    private void showFormError(String msg) {
        memFormErrorLabel.setText(msg);
        memFormErrorLabel.setVisible(true);
        memFormErrorLabel.setManaged(true);
    }

    private void hideFormError() {
        memFormErrorLabel.setText("");
        memFormErrorLabel.setVisible(false);
        memFormErrorLabel.setManaged(false);
    }


    @FXML
    private void handleNavDashboard(MouseEvent e) {
        navigate("DashboardView.fxml", "TCH — Dashboard");
    }

    @FXML
    private void handleNavClients(MouseEvent e) {
        navigate("ClientManagementView.fxml", "TCH — Clients");
    }

    @FXML
    private void handleNavWorkouts(MouseEvent e) {
        navigate("WorkoutTrackingView.fxml", "TCH — Workouts");
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
        var t = SessionManager.getInstance().getCurrentTrainer();
        if (t != null) avatarLabel.setText(String.valueOf(t.getName().charAt(0)).toUpperCase());
    }

    private void navigate(String fxml, String title) {
        Stage stage = (Stage) membershipTable.getScene().getWindow();
        ViewLoader.navigateTo(stage, fxml, title);
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private Optional<ButtonType> showConfirm(String title, String header, String content) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle(title);
        a.setHeaderText(header);
        a.setContentText(content);
        return a.showAndWait();
    }
}
