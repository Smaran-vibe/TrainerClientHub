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

public class AddTrainerDialogController implements Initializable {

    // Dialog fields
    @FXML
    private TextField nameField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField phoneField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label errorLabel;

    private final TrainerDAO trainerDAO = new TrainerDAO();

    @Override
    // No special setup needed for this dialog yet
    public void initialize(URL location, ResourceBundle resources) {

    }

    @FXML
    // Validate input and create a trainer record
    private void handleSave() {

        clearError();

        try {

            String name = nameField.getText().trim();
            String email = emailField.getText().trim();
            String phone = phoneField.getText().trim();
            String password = passwordField.getText();

            ValidationUtil.requireNonBlank(name, "Trainer name");
            ValidationUtil.requireValidEmail(email);
            ValidationUtil.requireValidNepalPhone(phone);
            ValidationUtil.requireValidPassword(password);


            if (trainerDAO.findByEmail(email.toLowerCase()).isPresent()) {
                showError("Email already registered. Please use a different email address.");
                return;
            }

            if (trainerDAO.findByEmailOrPhone(phone).isPresent()) {
                showError("Phone number already registered. Please use a different number.");
                return;
            }


            String passwordHash = hashPassword(password);
            Trainer trainer = new Trainer(name, email, phone, passwordHash);
            trainerDAO.insert(trainer);


            closeDialog();

        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (Exception e) {
            showError("An unexpected error occurred: " + e.getMessage());
        }
    }

    @FXML
    // Close without saving
    private void handleCancel() {
        closeDialog();
    }


    private String hashPassword(String plain) {
        return BCrypt.hashpw(plain, BCrypt.gensalt(12));
    }


    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }


    private void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }


    private void closeDialog() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }
}
