package com.trainerclienthub.controller;

import com.trainerclienthub.model.Client;
import com.trainerclienthub.model.Exercise;
import com.trainerclienthub.model.TrainerRole;
import com.trainerclienthub.model.Workout;
import com.trainerclienthub.service.ClientService;
import com.trainerclienthub.service.WorkoutService;
import com.trainerclienthub.util.SessionManager;
import com.trainerclienthub.util.ViewLoader;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;


public class WorkoutController implements Initializable {


    @FXML
    private Label avatarLabel;
    @FXML
    private HBox navMemberships;
    @FXML
    private HBox navPayments;
    @FXML
    private HBox navTrainers;


    @FXML
    private ComboBox<Client> clientSelector;


    @FXML
    private Button addWorkoutBtn;
    @FXML
    private Button deleteWorkoutBtn;


    @FXML
    private TableView<Workout> workoutTable;
    @FXML
    private TableColumn<Workout, Integer> colWorkoutId;
    @FXML
    private TableColumn<Workout, LocalDate> colWorkoutDate;
    @FXML
    private TableColumn<Workout, String> colWorkoutTrainer;
    @FXML
    private TableColumn<Workout, BigDecimal> colTotalVolume;
    @FXML
    private TableColumn<Workout, String> colWorkoutNotes;


    @FXML
    private Label exerciseSectionLabel;
    @FXML
    private Button addExerciseBtn;
    @FXML
    private Button deleteExerciseBtn;
    @FXML
    private TableView<Exercise> exerciseTable;
    @FXML
    private TableColumn<Exercise, String> colExerciseName;
    @FXML
    private TableColumn<Exercise, Integer> colSets;
    @FXML
    private TableColumn<Exercise, Integer> colReps;
    @FXML
    private TableColumn<Exercise, BigDecimal> colWeight;
    @FXML
    private TableColumn<Exercise, BigDecimal> colVolume;


    @FXML
    private VBox workoutFormPanel;
    @FXML
    private Label workoutFormTitle;
    @FXML
    private DatePicker fWorkoutDate;
    @FXML
    private TextArea fWorkoutNotes;
    @FXML
    private TextField fExerciseName;
    @FXML
    private TextField fSets;
    @FXML
    private TextField fReps;
    @FXML
    private TextField fWeightKg;
    @FXML
    private Button addExerciseInlineBtn;
    @FXML
    private ListView<String> exercisePreviewList;
    @FXML
    private Label workoutFormErrorLabel;
    @FXML
    private Button saveWorkoutBtn;
    @FXML
    private Button cancelWorkoutBtn;


    private final WorkoutService workoutService = new WorkoutService();
    private final ClientService clientService = new ClientService();

    private ObservableList<Workout> workouts = FXCollections.observableArrayList();
    private ObservableList<Exercise> exercises = FXCollections.observableArrayList();


    private final List<Exercise> stagedExercises = new ArrayList<>();
    private final ObservableList<String> stagedLabels = FXCollections.observableArrayList();


    @Override
    // Setup tables/forms and load workouts for the selected client
    public void initialize(URL location, ResourceBundle resources) {
        populateAvatarLabel();
        applyRoleBasedUI();
        configureWorkoutTable();
        configureExerciseTable();
        loadClientSelector();
        hideFormPanel();
        hideFormError();
        exercisePreviewList.setItems(stagedLabels);
    }

    // Trainers have a limited sidebar compared to admins
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


    private void configureWorkoutTable() {
        colWorkoutId.setCellValueFactory(new PropertyValueFactory<>("workoutId"));
        colWorkoutDate.setCellValueFactory(new PropertyValueFactory<>("workoutDate"));
        colTotalVolume.setCellValueFactory(new PropertyValueFactory<>("totalVolume"));
        colWorkoutNotes.setCellValueFactory(new PropertyValueFactory<>("notes"));

        colWorkoutTrainer.setCellValueFactory(new PropertyValueFactory<>("trainerName"));
        workoutTable.setItems(workouts);
    }

    private void configureExerciseTable() {
        colExerciseName.setCellValueFactory(new PropertyValueFactory<>("exerciseName"));
        colSets.setCellValueFactory(new PropertyValueFactory<>("sets"));
        colReps.setCellValueFactory(new PropertyValueFactory<>("reps"));
        colWeight.setCellValueFactory(new PropertyValueFactory<>("weightKg"));
        colVolume.setCellValueFactory(new PropertyValueFactory<>("volume"));
        exerciseTable.setItems(exercises);
    }

    private void loadClientSelector() {
        List<Client> clients = clientService.findAll();
        clientSelector.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Client c, boolean empty) {
                super.updateItem(c, empty);
                setText(empty || c == null ? null : c.getName());
            }
        });
        clientSelector.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Client c, boolean empty) {
                super.updateItem(c, empty);
                setText(empty || c == null ? "Select a client..." : c.getName());
            }
        });
        clientSelector.setItems(FXCollections.observableArrayList(clients));
    }


    @FXML
    private void handleClientSelected(ActionEvent event) {
        Client selected = clientSelector.getValue();
        if (selected == null) return;
        loadWorkoutsForClient(selected.getClientId());
        exercises.clear();
        hideExerciseActions();
    }

    // Load workouts for the current client selection
    private void loadWorkoutsForClient(int clientId) {
        workouts.setAll(workoutService.findByClient(clientId));
        exerciseSectionLabel.setText("Exercises — select a workout above");
    }


    @FXML
    private void handleWorkoutSelected(MouseEvent event) {
        Workout selected = workoutTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        exercises.setAll(selected.getExercises());
        exerciseSectionLabel.setText("Exercises — Workout " + selected.getWorkoutDate());
        showExerciseActions();
    }


    // Open the workout form for the selected client
    @FXML
    private void handleAddWorkout(ActionEvent event) {
        if (clientSelector.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "No Client", "Please select a client first.");
            return;
        }
        workoutFormTitle.setText("Log Workout");
        fWorkoutDate.setValue(LocalDate.now());
        clearWorkoutForm();
        showFormPanel();
    }

    @FXML
    private void handleDeleteWorkout(ActionEvent event) {
        Workout selected = workoutTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a workout to delete.");
            return;
        }
        Optional<ButtonType> result = showConfirm(
                "Delete Workout",
                "Delete workout from " + selected.getWorkoutDate() + "?",
                "All exercises in this workout will also be deleted.");
        if (result.isPresent() && result.get() == ButtonType.OK) {
            workoutService.deleteWorkout(selected.getWorkoutId());
            workouts.remove(selected);
            exercises.clear();
        }
    }


    @FXML
    private void handleAddExercise(ActionEvent event) {
        showFormPanel();
    }

    @FXML
    private void handleDeleteExercise(ActionEvent event) {
        Exercise selected = exerciseTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        showAlert(Alert.AlertType.INFORMATION, "Not Available",
                "Delete individual exercises by editing the workout.");
    }


    @FXML
    private void handleAddExerciseInline(ActionEvent event) {
        hideFormError();
        String name = fExerciseName.getText();
        String setsStr = fSets.getText();
        String repsStr = fReps.getText();
        String wgtStr = fWeightKg.getText();

        if (name == null || name.isBlank()) {
            showFormError("Exercise name is required.");
            return;
        }

        int sets, reps;
        BigDecimal wgt;
        try {
            sets = Integer.parseInt(setsStr.trim());
        } catch (NumberFormatException e) {
            showFormError("Sets must be a whole number.");
            return;
        }
        try {
            reps = Integer.parseInt(repsStr.trim());
        } catch (NumberFormatException e) {
            showFormError("Reps must be a whole number.");
            return;
        }
        try {
            wgt = new BigDecimal(wgtStr.trim());
        } catch (NumberFormatException e) {
            showFormError("Weight must be a number (e.g. 60.0).");
            return;
        }

        if (sets <= 0) {
            showFormError("Sets must be greater than 0.");
            return;
        }
        if (reps <= 0) {
            showFormError("Reps must be greater than 0.");
            return;
        }
        if (wgt.compareTo(BigDecimal.ZERO) <= 0) {
            showFormError("Weight must be greater than 0.");
            return;
        }


        Exercise ex = new Exercise();
        ex.setExerciseName(name);
        ex.setSets(sets);
        ex.setReps(reps);
        ex.setWeightKg(wgt);
        stagedExercises.add(ex);
        stagedLabels.add(name + "  " + sets + "×" + reps + " @ " + wgt + " kg  ["
                + ex.getVolume().toPlainString() + " kg vol]");
        fExerciseName.clear();
        fSets.clear();
        fReps.clear();
        fWeightKg.clear();
        fExerciseName.requestFocus();
    }

    @FXML
    // Save the workout and its exercises
    private void handleSaveWorkout(ActionEvent event) {
        hideFormError();

        Client client = clientSelector.getValue();
        if (client == null) {
            showFormError("Please select a client first.");
            return;
        }
        if (client.getSessionBalance() <= 0) {
            showAlert(Alert.AlertType.WARNING, "No Sessions Remaining",
                    "No sessions remaining to log workout");
            return;
        }

        LocalDate date = fWorkoutDate.getValue();
        if (date == null) {
            showFormError("Please select a workout date.");
            return;
        }
        if (date.isAfter(LocalDate.now())) {
            showFormError("Workout date cannot be in the future.");
            return;
        }
        if (stagedExercises.isEmpty()) {
            showFormError("Add at least one exercise before saving.");
            return;
        }

        int trainerId = SessionManager.getInstance().getCurrentTrainer().getTrainerId();

        saveWorkoutBtn.setDisable(true);
        saveWorkoutBtn.setText("Saving...");
        try {
            Workout saved = workoutService.logWorkout(
                    client.getClientId(), trainerId, date,
                    fWorkoutNotes.getText(), stagedExercises);
            workouts.add(0, saved);
            clearWorkoutForm();
            hideFormPanel();
            showAlert(Alert.AlertType.INFORMATION, "Workout Saved",
                    "Workout logged successfully for " + client.getName() + ".\n"
                            + saved.getExercises().size() + " exercise(s) — Total volume: "
                            + saved.getTotalVolume().toPlainString() + " kg.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            showFormError(ex.getMessage());
            showAlert(Alert.AlertType.ERROR, "Save Failed", ex.getMessage());
        } catch (Exception ex) {
            String msg = "Unexpected error: " + ex.getMessage();
            showFormError(msg);
            showAlert(Alert.AlertType.ERROR, "Error", msg);
        } finally {
            saveWorkoutBtn.setDisable(false);
            saveWorkoutBtn.setText("Save Workout");
        }
    }

    @FXML
    private void handleCancelWorkoutForm(ActionEvent event) {
        clearWorkoutForm();
        hideFormPanel();
    }


    private void clearWorkoutForm() {
        fWorkoutNotes.clear();
        fExerciseName.clear();
        fSets.clear();
        fReps.clear();
        fWeightKg.clear();
        stagedExercises.clear();
        stagedLabels.clear();
        hideFormError();
    }

    private void showFormPanel() {
        workoutFormPanel.setVisible(true);
        workoutFormPanel.setManaged(true);
    }

    private void hideFormPanel() {
        workoutFormPanel.setVisible(false);
        workoutFormPanel.setManaged(false);
    }

    private void showExerciseActions() {
        addExerciseBtn.setVisible(true);
        addExerciseBtn.setManaged(true);
        deleteExerciseBtn.setVisible(true);
        deleteExerciseBtn.setManaged(true);
    }

    private void hideExerciseActions() {
        addExerciseBtn.setVisible(false);
        addExerciseBtn.setManaged(false);
        deleteExerciseBtn.setVisible(false);
        deleteExerciseBtn.setManaged(false);
    }

    private void showFormError(String msg) {
        workoutFormErrorLabel.setText(msg);
        workoutFormErrorLabel.setVisible(true);
        workoutFormErrorLabel.setManaged(true);
    }

    private void hideFormError() {
        workoutFormErrorLabel.setText("");
        workoutFormErrorLabel.setVisible(false);
        workoutFormErrorLabel.setManaged(false);
    }

    private void populateAvatarLabel() {
        var trainer = SessionManager.getInstance().getCurrentTrainer();
        if (trainer != null) avatarLabel.setText(String.valueOf(trainer.getName().charAt(0)).toUpperCase());
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

    private void navigate(String fxml, String title) {
        Stage stage = (Stage) workoutTable.getScene().getWindow();
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
