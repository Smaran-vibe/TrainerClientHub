package com.trainerclienthub.controller;

import com.trainerclienthub.service.TrainerService;
import com.trainerclienthub.util.ViewLoader;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class RegisterController implements Initializable {

    //  FXML injections

    @FXML private TextField     nameField;
    @FXML private TextField     emailField;
    @FXML private TextField     phoneField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button        registerButton;
    @FXML private Hyperlink     loginLink;
    @FXML private Label         errorLabel;

    //  Services

    private final TrainerService trainerService = new TrainerService();

    //  Initialise

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        clearError();
        Platform.runLater(() -> nameField.requestFocus());

        // Clear inline error whenever the user edits any field
        nameField.textProperty().addListener((o, ov, nv) -> clearError());
        emailField.textProperty().addListener((o, ov, nv) -> clearError());
        phoneField.textProperty().addListener((o, ov, nv) -> clearError());
        passwordField.textProperty().addListener((o, ov, nv) -> clearError());
        confirmPasswordField.textProperty().addListener((o, ov, nv) -> clearError());
    }

    //  Event handlers

    @FXML
    private void handleRegister(ActionEvent event) {
        clearError();

        String name    = nameField.getText();
        String email   = emailField.getText();
        String phone   = phoneField.getText();
        String pass    = passwordField.getText();
        String confirm = confirmPasswordField.getText();

        // UI-level checks (fast, no DB call)
        if (name == null || name.isBlank()) {
            showFieldError("Please enter your full name.", nameField);
            return;
        }
        if (email == null || email.isBlank()
                || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            showFieldError("Please enter a valid email address.", emailField);
            return;
        }
        if (phone == null || !phone.trim().matches("^(\\+977|977)?[0-9]{10}$")) {
            showFieldError(
                    "Enter a valid Nepal phone number: +977XXXXXXXXXX or 10 digits.",
                    phoneField);
            return;
        }
        if (pass == null || pass.length() < 8 ) {
            showFieldError("Password must be at least 8 characters.", passwordField);
            return;
        }


        if (!pass.matches(".*[^a-zA-Z0-9].*")) {
            showFieldError("Password must contain at least one special character.", passwordField);
            return;
        }


        if (!pass.equals(confirm)) {
            confirmPasswordField.clear();
            showFieldError("Passwords do not match.", confirmPasswordField);
            return;
        }

// ── Disable button to prevent double-submit ───────────────────────────
        registerButton.setDisable(true);
        registerButton.setText("Creating account...");

        try {
            trainerService.register(name, email, phone, pass, confirm);


            showAlert(Alert.AlertType.INFORMATION,
                    "Registration Successful",
                    "Account created successfully!\n\n"
                            + "You can now log in with your email address or phone number.");

            Stage stage = (Stage) registerButton.getScene().getWindow();
            ViewLoader.navigateTo(stage, "LoginView.fxml", "TCH — Login");

        } catch (IllegalArgumentException ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : "Failed to create account.";
            showInlineError(msg);

            String lower = msg.toLowerCase();
            if (lower.contains("email already registered")) {
                showAlert(Alert.AlertType.ERROR, "Email Already Registered",
                        "Email already registered.\n\n"
                                + "Please use a different email address or log in instead.");
            } else if (lower.contains("phone number already registered")) {
                showAlert(Alert.AlertType.ERROR, "Phone Already Registered",
                        "Phone number already registered.\n\n"
                                + "Please use a different phone number or log in instead.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Registration Failed",
                        "Failed to create account.\n\n" + msg);
            }

        } catch (Exception ex) {
            String msg = "An unexpected error occurred. Please try again.";
            showInlineError(msg);
            showAlert(Alert.AlertType.ERROR, "Unexpected Error", msg);
            ex.printStackTrace();

        } finally {
            registerButton.setDisable(false);
            registerButton.setText("CREATE ACCOUNT");
        }
    }

    @FXML
    private void handleLoginLink(ActionEvent event) {
        Stage stage = (Stage) loginLink.getScene().getWindow();
        ViewLoader.navigateTo(stage, "LoginView.fxml", "TCH — Login");
    }

    //  Private helpers

    private void showFieldError(String message, Control field) {
        showInlineError(message);
        field.requestFocus();
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

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}