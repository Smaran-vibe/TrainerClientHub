package com.trainerclienthub.controller;

import com.trainerclienthub.DAO.TrainerDAO;
import com.trainerclienthub.model.Trainer;
import com.trainerclienthub.util.ValidationUtil;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.mindrot.jbcrypt.BCrypt;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the Add Trainer dialog.
 * Handles validation and saving of new trainer records.
 */
public class AddTrainerDialogController implements Initializable {

    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private final TrainerDAO trainerDAO = new TrainerDAO();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialization logic if needed
    }

    @FXML
    private void handleSave() {
        // Clear previous error
        clearError();

        try {
            // Validate inputs
            String name = nameField.getText().trim();
            String email = emailField.getText().trim();
            String phone = phoneField.getText().trim();
            String password = passwordField.getText();

            ValidationUtil.requireNonBlank(name, "Trainer name");
            ValidationUtil.requireValidEmail(email);
            ValidationUtil.requireValidNepalPhone(phone);
            ValidationUtil.requireValidPassword(password);

            // Check for duplicates
            if (trainerDAO.findByEmail(email.toLowerCase()).isPresent()) {
                showError("Email already registered. Please use a different email address.");
                return;
            }

            if (trainerDAO.findByEmailOrPhone(phone).isPresent()) {
                showError("Phone number already registered. Please use a different number.");
                return;
            }

            // Create and save the trainer
            String passwordHash = hashPassword(password);
            Trainer trainer = new Trainer(name, email, phone, passwordHash);
            trainerDAO.insert(trainer);

            // Close the dialog on success
            closeDialog();

        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (Exception e) {
            showError("An unexpected error occurred: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    /**
     * Hash the plain-text password using BCrypt.
     */
    private String hashPassword(String plain) {
        return BCrypt.hashpw(plain, BCrypt.gensalt(12));
    }

    /**
     * Display an error message in the error label.
     */
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    /**
     * Clear the error message.
     */
    private void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    /**
     * Close the dialog window.
     */
    private void closeDialog() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }
}

