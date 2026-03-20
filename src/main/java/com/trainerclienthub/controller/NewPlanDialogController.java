package com.trainerclienthub.controller;

import com.trainerclienthub.model.MembershipPlan;
import com.trainerclienthub.model.PlanType;
import com.trainerclienthub.service.MembershipService;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.function.Consumer;

public class NewPlanDialogController {

    @FXML private TextField planNameField;
    @FXML private ComboBox<PlanType> planTypeCombo;
    @FXML private TextField durationField;
    @FXML private TextField priceField;
    @FXML private TextField sessionsField;
    @FXML private Label errorLabel;

    private Stage stage;
    private MembershipService membershipService;
    private Consumer<MembershipPlan> onPlanCreated;
    private MembershipPlan existingPlan;

    @FXML
    private void initialize() {
        planTypeCombo.setItems(FXCollections.observableArrayList(PlanType.values()));
        planTypeCombo.setValue(PlanType.MONTHLY);
        hideError();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setMembershipService(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    public void setOnPlanCreated(Consumer<MembershipPlan> onPlanCreated) {
        this.onPlanCreated = onPlanCreated;
    }

    public void setExistingPlan(MembershipPlan plan) {
        if (plan == null) return;
        this.existingPlan = plan;
        planNameField.setText(plan.getPlanName());
        planTypeCombo.setValue(plan.getPlanType());
        durationField.setText(String.valueOf(plan.getDurationDays()));
        priceField.setText(plan.getPrice().toPlainString());
        sessionsField.setText(String.valueOf(plan.getSessionsIncluded()));
    }

    @FXML
    private void handleSave(ActionEvent event) {
        hideError();
        try {
            validateDependencies();
            String name = planNameField.getText().trim();
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Plan name must not be empty.");
            }

            PlanType type = planTypeCombo.getValue();
            if (type == null) {
                throw new IllegalArgumentException("Select a plan type.");
            }

            int duration = Integer.parseInt(durationField.getText().trim());
            if (duration <= 0) {
                throw new IllegalArgumentException("Duration must be greater than zero.");
            }

            BigDecimal price = new BigDecimal(priceField.getText().trim());
            if (price.signum() < 0) {
                throw new IllegalArgumentException("Price must be zero or greater.");
            }

            int sessions = Integer.parseInt(sessionsField.getText().trim());
            if (sessions <= 0) {
                throw new IllegalArgumentException("Sessions included must be at least one.");
            }

            MembershipPlan plan;
            if (existingPlan == null) {
                plan = new MembershipPlan(name, type, duration, price, sessions);
                membershipService.createPlan(plan);
            } else {
                plan = new MembershipPlan(existingPlan.getPlanId(), name, type, duration, price, sessions);
                membershipService.updatePlan(plan);
            }
            if (onPlanCreated != null) {
                onPlanCreated.accept(plan);
            }
            stage.close();
        } catch (NumberFormatException ex) {
            showError("Duration, price and sessions must be numeric values.");
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        if (stage != null) {
            stage.close();
        }
    }

    private void validateDependencies() {
        Objects.requireNonNull(stage, "Dialog stage must be set.");
        Objects.requireNonNull(membershipService, "MembershipService must be injected.");
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
