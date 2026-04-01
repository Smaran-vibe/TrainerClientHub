package com.trainerclienthub.controller;

import com.trainerclienthub.model.Trainer;
import com.trainerclienthub.service.TrainerService;
import com.trainerclienthub.util.SessionManager;
import com.trainerclienthub.util.ViewLoader;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {



    /**
     * Single field — accepts either an email address or a Nepal phone number.
     */
    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField passwordVisibleField;
    @FXML
    private ToggleButton showPasswordToggle;
    @FXML
    private Button loginButton;
    @FXML
    private Hyperlink registerLink;
    @FXML
    private Label errorLabel;

    //  Services

    private final TrainerService trainerService = new TrainerService();

    //  Initialise

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        clearError();

        // Enter key on either field fires the login button
        emailField.setOnKeyPressed(this::handleKeyPress);
        passwordField.setOnKeyPressed(this::handleKeyPress);

        // Clear inline error as soon as the user starts correcting input
        emailField.textProperty().addListener((obs, o, n) -> clearError());
        passwordField.textProperty().addListener((obs, o, n) -> clearError());

        // Auto-focus the identifier field when the view loads
        Platform.runLater(() -> emailField.requestFocus());

        passwordVisibleField.managedProperty().bind(showPasswordToggle.selectedProperty());
        passwordVisibleField.visibleProperty().bind(showPasswordToggle.selectedProperty());
        passwordField.managedProperty().bind(showPasswordToggle.selectedProperty().not());
        passwordField.visibleProperty().bind(showPasswordToggle.selectedProperty().not());
        passwordVisibleField.textProperty().bindBidirectional(passwordField.textProperty());
    }

    //  Event handlers

    @FXML
    private void handleLogin(ActionEvent event) {
        clearError();

        String identifier = emailField.getText();
        String password = passwordField.getText();

        //  Both fields must be filled before we hit the database
        if (identifier == null || identifier.isBlank()
                || password == null || password.isBlank()) {
            String msg = "Please enter your email/phone number and password.";
            showInlineError(msg);
            showAlert(Alert.AlertType.WARNING, "Login Required", msg);
            return;
        }

        setFormLoading(true);

        try {
            // Delegate — service accepts email or phone, returns Trainer with role
            Trainer trainer = trainerService.authenticate(identifier, password);

            SessionManager.getInstance().login(trainer);

            Stage stage = (Stage) loginButton.getScene().getWindow();
            String title = trainer.isAdmin() ? "TCH — Admin Dashboard" : "TCH — Dashboard";
            ViewLoader.navigateTo(stage, "DashboardView.fxml", title);

        } catch (IllegalArgumentException ex) {

            String msg = ex.getMessage() != null ? ex.getMessage() : "Login failed.";
            showInlineError(msg);
            passwordField.clear();
            passwordField.requestFocus();

            // Map each distinct error to the most appropriate Alert type
            String lowerMsg = msg.toLowerCase();
            if (lowerMsg.contains("account does not exist")) {
                showAlert(Alert.AlertType.ERROR, "Account Not Found",
                        "Account does not exist.\n\n"
                                + "Check your email or phone number and try again.");
            } else if (lowerMsg.contains("incorrect password")) {
                showAlert(Alert.AlertType.ERROR, "Incorrect Password",
                        "Incorrect password. Please try again.");
            } else {
                showAlert(Alert.AlertType.WARNING, "Login Failed", msg);
            }

        } catch (Exception ex) {
            String msg = "An unexpected error occurred. Please try again.";
            showInlineError(msg);
            showAlert(Alert.AlertType.ERROR, "Unexpected Error", msg);
            ex.printStackTrace();

        } finally {
            setFormLoading(false);
        }
    }

    @FXML
    private void handleRegisterLink(ActionEvent event) {
        Stage stage = (Stage) registerLink.getScene().getWindow();
        ViewLoader.navigateTo(stage, "RegisterView.fxml", "TCH — Create Account");
    }

    // Private helpers

    private void handleKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            loginButton.fire();
        }
    }

    private void showInlineError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void setFormLoading(boolean loading) {
        loginButton.setDisable(loading);
        loginButton.setText(loading ? "Logging in..." : "LOGIN");
    }

    /**
     * Shows a modal JavaFX Alert dialog.
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleShowPasswordToggle(ActionEvent event) {



        boolean showing = showPasswordToggle.isSelected();
        TextField focusTarget = showing ? passwordVisibleField : passwordField;

        focusTarget.requestFocus();
        focusTarget.positionCaret(focusTarget.getText().length());
    }
}
