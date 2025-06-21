package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.Goal;
import models.User;
import services.GoalService;

import java.time.LocalDate;

public class GoalDialogController {

    @FXML private Label titleLabel;
    @FXML private ComboBox<Goal.GoalType> goalTypeCombo;
    @FXML private TextArea descriptionField;
    @FXML private TextField targetValueField, targetUnitField, currentValueField;
    @FXML private DatePicker startDatePicker, targetDatePicker;
    @FXML private ComboBox<Goal.Priority> priorityCombo;
    @FXML private Button saveBtn, cancelBtn;

    private User currentUser;
    private Goal editingGoal;
    private Stage dialogStage;
    private boolean goalSaved = false;

    private GoalService goalService = new GoalService();

    @FXML
    public void initialize() {
        // Populate ComboBoxes
        goalTypeCombo.getItems().setAll(Goal.GoalType.values());
        priorityCombo.getItems().setAll(Goal.Priority.values());

        // Default values
        startDatePicker.setValue(LocalDate.now());
        priorityCombo.setValue(Goal.Priority.MEDIUM);

        // Button actions
        saveBtn.setOnAction(e -> handleSave());
        cancelBtn.setOnAction(e -> dialogStage.close());
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        System.out.println("🔧 Current user set: " + (user != null ? user.getId() : "null"));
    }

    public void setGoal(Goal goal) {
        this.editingGoal = goal;

        if (goal != null) {
            titleLabel.setText("✏️ Edit Goal");
            goalTypeCombo.setValue(goal.getGoalType());
            descriptionField.setText(goal.getGoalDescription());
            targetValueField.setText(goal.getTargetValue() != null ? goal.getTargetValue().toString() : "");
            targetUnitField.setText(goal.getTargetUnit());
            currentValueField.setText(goal.getCurrentValue() != null ? goal.getCurrentValue().toString() : "");
            startDatePicker.setValue(goal.getStartDate());
            targetDatePicker.setValue(goal.getTargetDate());
            priorityCombo.setValue(goal.getPriority());
        } else {
            titleLabel.setText("🎯 Add New Goal");
        }
    }

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    public boolean isGoalSaved() {
        return goalSaved;
    }

    private void handleSave() {
        System.out.println("🔄 Handling save...");

        // Validate input trước
        if (!validateInput()) {
            System.out.println("❌ Validation failed");
            return;
        }

        try {
            // Prepare Goal object
            Goal goal = editingGoal != null ? editingGoal : new Goal();

            // Set basic properties
            goal.setUserId(currentUser.getId());
            goal.setGoalType(goalTypeCombo.getValue());
            goal.setGoalDescription(descriptionField.getText().trim());
            goal.setTargetUnit(targetUnitField.getText().trim());
            goal.setStartDate(startDatePicker.getValue());
            goal.setTargetDate(targetDatePicker.getValue());
            goal.setPriority(priorityCombo.getValue());

            // Parse target value safely
            String targetValueText = targetValueField.getText().trim();
            if (!targetValueText.isEmpty()) {
                try {
                    Double targetValue = Double.parseDouble(targetValueText);
                    goal.setTargetValue(targetValue);
                    System.out.println("✅ Target value parsed: " + targetValue);
                } catch (NumberFormatException e) {
                    showErrorAlert("Target value must be a valid number!");
                    return;
                }
            } else {
                goal.setTargetValue(0.0);
            }

            // Parse current value safely
            String currentValueText = currentValueField.getText().trim();
            if (!currentValueText.isEmpty()) {
                try {
                    Double currentValue = Double.parseDouble(currentValueText);
                    goal.setCurrentValue(currentValue);
                    System.out.println("✅ Current value parsed: " + currentValue);
                } catch (NumberFormatException e) {
                    showErrorAlert("Current value must be a valid number!");
                    return;
                }
            } else {
                goal.setCurrentValue(0.0);
            }

            // Debug: In ra thông tin goal trước khi lưu
            System.out.println("🔧 Goal info before save:");
            System.out.println("- User ID: " + goal.getUserId());
            System.out.println("- Goal Type: " + goal.getGoalType());
            System.out.println("- Description: " + goal.getGoalDescription());
            System.out.println("- Target Value: " + goal.getTargetValue());
            System.out.println("- Current Value: " + goal.getCurrentValue());
            System.out.println("- Target Unit: " + goal.getTargetUnit());
            System.out.println("- Start Date: " + goal.getStartDate());
            System.out.println("- Target Date: " + goal.getTargetDate());
            System.out.println("- Priority: " + goal.getPriority());

            // Save or update
            boolean success;
            if (editingGoal == null) {
                System.out.println("🔄 Creating new goal...");
                success = goalService.createGoal(goal);
            } else {
                System.out.println("🔄 Updating existing goal...");
                success = goalService.updateGoal(goal);
            }

            if (success) {
                goalSaved = true;
                System.out.println("✅ Goal saved successfully!");
                showSuccessAlert("Goal saved successfully!");
                dialogStage.close();
            } else {
                System.out.println("❌ Failed to save goal!");
                showErrorAlert("Failed to save goal! Please check the console for details.");
            }

        } catch (Exception e) {
            System.err.println("❌ Unexpected error in handleSave: " + e.getMessage());
            e.printStackTrace();
            showErrorAlert("An unexpected error occurred: " + e.getMessage());
        }
    }

    private boolean validateInput() {
        System.out.println("🔄 Validating input...");

        // Check current user
        if (currentUser == null) {
            System.out.println("❌ Current user is null!");
            showErrorAlert("User information is missing!");
            return false;
        }

        // Check goal type
        if (goalTypeCombo.getValue() == null) {
            System.out.println("❌ Goal type not selected");
            showErrorAlert("Please select a goal type.");
            goalTypeCombo.requestFocus();
            return false;
        }

        // Check description
        if (descriptionField.getText() == null || descriptionField.getText().trim().isEmpty()) {
            System.out.println("❌ Description is empty");
            showErrorAlert("Please enter a goal description.");
            descriptionField.requestFocus();
            return false;
        }

        // Check target value
        String targetValueText = targetValueField.getText();
        if (targetValueText == null || targetValueText.trim().isEmpty()) {
            System.out.println("❌ Target value is empty");
            showErrorAlert("Please enter a target value.");
            targetValueField.requestFocus();
            return false;
        }

        // Validate target value is a number
        try {
            Double.parseDouble(targetValueText.trim());
        } catch (NumberFormatException e) {
            System.out.println("❌ Target value is not a valid number: " + targetValueText);
            showErrorAlert("Target value must be a valid number.");
            targetValueField.requestFocus();
            return false;
        }

        // Check target unit
        if (targetUnitField.getText() == null || targetUnitField.getText().trim().isEmpty()) {
            System.out.println("❌ Target unit is empty");
            showErrorAlert("Please enter a unit for the target value.");
            targetUnitField.requestFocus();
            return false;
        }

        // Check current value if provided
        String currentValueText = currentValueField.getText();
        if (currentValueText != null && !currentValueText.trim().isEmpty()) {
            try {
                Double.parseDouble(currentValueText.trim());
            } catch (NumberFormatException e) {
                System.out.println("❌ Current value is not a valid number: " + currentValueText);
                showErrorAlert("Current value must be a valid number.");
                currentValueField.requestFocus();
                return false;
            }
        }

        // Check dates
        if (startDatePicker.getValue() == null) {
            System.out.println("❌ Start date not selected");
            showErrorAlert("Please select a start date.");
            startDatePicker.requestFocus();
            return false;
        }

        if (targetDatePicker.getValue() == null) {
            System.out.println("❌ Target date not selected");
            showErrorAlert("Please select a target date.");
            targetDatePicker.requestFocus();
            return false;
        }

        if (targetDatePicker.getValue().isBefore(startDatePicker.getValue())) {
            System.out.println("❌ Target date is before start date");
            showErrorAlert("Target date cannot be before start date.");
            targetDatePicker.requestFocus();
            return false;
        }

        // Check priority
        if (priorityCombo.getValue() == null) {
            System.out.println("❌ Priority not selected");
            showErrorAlert("Please select a priority level.");
            priorityCombo.requestFocus();
            return false;
        }

        System.out.println("✅ Validation passed!");
        return true;
    }

    private void showSuccessAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}