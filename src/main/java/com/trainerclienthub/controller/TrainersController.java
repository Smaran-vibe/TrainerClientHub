package com.trainerclienthub.controller;

import com.trainerclienthub.DAO.TrainerDAO;
import com.trainerclienthub.model.Trainer;
import com.trainerclienthub.model.TrainerRole;
import com.trainerclienthub.util.SessionManager;
import com.trainerclienthub.util.ViewLoader;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;

import java.net.URL;
import java.util.ResourceBundle;

public class TrainersController implements Initializable {

    @FXML
    private Label avatarLabel;
    @FXML
    private SidebarController sidebarController;
    @FXML
    private Button addTrainerBtn;

    @FXML
    private TableView<Trainer> trainerTable;
    @FXML
    private TableColumn<Trainer, Integer> colTrainerId;
    @FXML
    private TableColumn<Trainer, String> colTrainerName;
    @FXML
    private TableColumn<Trainer, String> colTrainerEmail;
    @FXML
    private TableColumn<Trainer, String> colTrainerPhone;
    @FXML
    private TableColumn<Trainer, Trainer> colTrainerStatusAction;

    private final TrainerDAO trainerDAO = new TrainerDAO();
    private final ObservableList<Trainer> trainers = FXCollections.observableArrayList();

    @Override
    // Setup table and load trainer list
    public void initialize(URL location, ResourceBundle resources) {
        applyRoleBasedUI();
        configureTable();
        loadTrainers();
        populateAvatarLabel();
        if (sidebarController != null) {
            sidebarController.highlight("trainers");
        }
    }

    // Block trainer accounts from accessing this screen
    private void applyRoleBasedUI() {
        boolean isTrainer = SessionManager.getInstance().getRole() == TrainerRole.TRAINER;
        if (isTrainer) {
            showAlert(Alert.AlertType.WARNING,
                    "Access Denied",
                    "Trainer accounts cannot access the Trainers directory.");
            redirectToDashboard();
        }
    }

    private void redirectToDashboard() {
        Stage stage = (Stage) avatarLabel.getScene().getWindow();
        ViewLoader.navigateTo(stage, "DashboardView.fxml", "TCH — Dashboard");
    }

    private void configureTable() {
        colTrainerId.setCellValueFactory(new PropertyValueFactory<>("trainerId"));
        colTrainerName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colTrainerEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colTrainerPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colTrainerStatusAction.setCellValueFactory(data ->
                new SimpleObjectProperty<>(data.getValue()));


        colTrainerStatusAction.setStyle("-fx-alignment: CENTER-RIGHT;");

        colTrainerStatusAction.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Trainer trainer, boolean empty) {
                super.updateItem(trainer, empty);
                if (empty || trainer == null) {
                    setGraphic(null);
                    return;
                }

                Label statusLabel = new Label(trainer.isAdmin() ? "Admin" : "Trainer");
                statusLabel.getStyleClass().removeAll("text-success", "text-secondary");
                statusLabel.getStyleClass().add(trainer.isAdmin() ? "text-success" : "text-secondary");


                javafx.scene.shape.SVGPath eyeIcon = new javafx.scene.shape.SVGPath();
                eyeIcon.setContent("M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z");
                eyeIcon.setFill(javafx.scene.paint.Color.web("#CCFF00"));

                Button detailButton = new Button();
                detailButton.setGraphic(eyeIcon);
                detailButton.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0;");

                detailButton.setOnAction(event ->
                        showAlert(Alert.AlertType.INFORMATION,
                                "Trainer Details",
                                trainer.getName() + " • " + trainer.getEmail() + "\nPhone: " + trainer.getPhone()));


                javafx.scene.shape.SVGPath trashIcon = new javafx.scene.shape.SVGPath();
                trashIcon.setContent("M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z");
                trashIcon.setFill(javafx.scene.paint.Color.web("#FF3333"));

                Button deleteButton = new Button();
                deleteButton.setGraphic(trashIcon);
                deleteButton.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0;");

                deleteButton.setOnAction(event -> deleteTrainer(trainer));
                HBox container = new HBox(15, statusLabel, detailButton, deleteButton);
                container.setAlignment(Pos.CENTER_RIGHT);
                setGraphic(container);
            }
        });

        trainerTable.setItems(trainers);
        trainerTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        trainerTable.setPlaceholder(new Label("No trainers registered yet."));
    }

    // Pull trainers from DAO and refresh the table
    private void loadTrainers() {
        trainers.setAll(trainerDAO.getAllTrainers());
    }

    private void populateAvatarLabel() {
        var current = SessionManager.getInstance().getCurrentTrainer();
        if (current != null && current.getName() != null && !current.getName().isBlank()) {
            avatarLabel.setText(current.getName().substring(0, 1).toUpperCase());
        }
    }

    @FXML
    // Opens the "Add Trainer" modal dialog
    private void handleAddTrainer() {
        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/trainerclienthub/AddTrainerDialog.fxml"));
            javafx.scene.layout.BorderPane dialogRoot = loader.load();


            Stage dialogStage = new Stage();
            dialogStage.setTitle("Add New Trainer");
            dialogStage.setScene(new Scene(dialogRoot));
            dialogStage.setResizable(false);
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initStyle(StageStyle.UTILITY);
            dialogStage.initOwner((Stage) avatarLabel.getScene().getWindow());


            dialogStage.showAndWait();


            loadTrainers();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR,
                    "Error",
                    "Failed to open Add Trainer dialog: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void deleteTrainer(Trainer trainer) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirm Delete");
        confirmDialog.setHeaderText(null);
        confirmDialog.setContentText("Are you sure you want to delete " + trainer.getName() + "?");

        if (confirmDialog.showAndWait().isPresent() && confirmDialog.getResult() == javafx.scene.control.ButtonType.OK) {
            try {
                trainerDAO.delete(trainer.getTrainerId());
                loadTrainers();
                showAlert(Alert.AlertType.INFORMATION, "Success", "Trainer deleted successfully.");
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete trainer: " + e.getMessage());
            }
        }
    }
}
