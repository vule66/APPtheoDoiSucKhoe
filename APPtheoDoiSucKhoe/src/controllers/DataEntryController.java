package controllers;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Duration;
import models.HealthEntry;
import models.User;
import services.HealthEntryService;
import services.UserService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class DataEntryController {

    @FXML private DatePicker datePicker;
    @FXML private TextField weightField;
    @FXML private TextField bloodPressureField;
    @FXML private TextField sleepField;
    @FXML private Button saveButton;
    @FXML private Label statusLabel;
    @FXML private Text bmiValueLabel;
    @FXML private Text bmiCategoryLabel;
    @FXML private Text weightChangeLabel;
    @FXML private VBox historyVBox;

    private User currentUser;
    private final HealthEntryService entryService = new HealthEntryService();
    private final UserService userService = new UserService();

    @FXML
    public void initialize() {
        datePicker.setValue(LocalDate.now());
        datePicker.valueProperty().addListener((obs, oldDate, newDate) -> {
            if (newDate != null && currentUser != null) {
                loadEntryForDate(newDate);
            }
        });
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        // Tải lại thông tin user mới nhất từ DB để có chiều cao
        this.currentUser = userService.findUserById(user.getId()).orElse(user);

        loadEntryForDate(datePicker.getValue());
        updateSidePanel();
    }

    @FXML
    private void saveEntry() {
        if (currentUser == null) {
            showStatusMessage("Error: No user logged in.", true);
            return;
        }
        LocalDate entryDate = datePicker.getValue();
        if (entryDate == null) {
            showStatusMessage("Error: Please select a date.", true);
            return;
        }

        Double weight = null;
        String weightText = weightField.getText().trim();
        if (!weightText.isEmpty()) {
            try {
                weight = Double.parseDouble(weightText.replace(',', '.'));
            } catch (NumberFormatException e) {
                showStatusMessage("Error: Invalid number for Weight.", true);
                return;
            }
        }

        Double sleep = null;
        String sleepText = sleepField.getText().trim();
        if (!sleepText.isEmpty()) {
            try {
                sleep = Double.parseDouble(sleepText.replace(',', '.'));
            } catch (NumberFormatException e) {
                showStatusMessage("Error: Invalid number for Hours Slept.", true);
                return;
            }
        }

        String bloodPressure = bloodPressureField.getText().trim();
        if (bloodPressure.isEmpty()) {
            bloodPressure = null;
        }

        HealthEntry entry = new HealthEntry(currentUser.getId(), entryDate, weight, bloodPressure, sleep);
        try {
            entryService.saveOrUpdateEntry(entry);
            showStatusMessage("Entry saved successfully!", false);
            updateSidePanel();
        } catch (SQLException e) {
            showStatusMessage("Database could not save entry.", true);
            e.printStackTrace();
        }
    }

    private void loadEntryForDate(LocalDate date) {
        if (currentUser == null) return;
        try {
            Optional<HealthEntry> entryOpt = entryService.findEntryByUserIdAndDate(currentUser.getId(), date);
            if (entryOpt.isPresent()) {
                HealthEntry entry = entryOpt.get();
                weightField.setText(entry.getWeightKg() != null ? String.valueOf(entry.getWeightKg()) : "");
                bloodPressureField.setText(entry.getBloodPressure() != null ? entry.getBloodPressure() : "");
                sleepField.setText(entry.getHoursSlept() != null ? String.valueOf(entry.getHoursSlept()) : "");
            } else {
                clearForm();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showStatusMessage("Error: Could not load data.", true);
        }
    }

    private void updateSidePanel() {
        if (currentUser == null) return;
        try {
            List<HealthEntry> recentEntries = entryService.findRecentEntries(currentUser.getId(), 5);
            updateBmi(recentEntries);
            updateWeightChange(recentEntries);
            populateHistory(recentEntries);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateBmi(List<HealthEntry> recentEntries) {
        // <<< ĐÂY LÀ DÒNG ĐÃ SỬA LỖI >>>
        // Đã xóa bỏ `currentUser.getHeight() == null` vì getHeight() trả về kiểu `double`
        if (currentUser.getHeight() <= 0 || recentEntries.isEmpty() || recentEntries.get(0).getWeightKg() == null) {
            bmiValueLabel.setText("--");
            bmiCategoryLabel.setText("Need height & weight");
            return;
        }
        Double latestWeight = recentEntries.get(0).getWeightKg();
        double heightM = currentUser.getHeight() / 100.0;
        double bmi = latestWeight / (heightM * heightM);
        bmiValueLabel.setText(String.format("%.1f", bmi));

        String category;
        if (bmi < 18.5) category = "Underweight";
        else if (bmi < 24.9) category = "Normal";
        else if (bmi < 29.9) category = "Overweight";
        else category = "Obese";
        bmiCategoryLabel.setText(category);
    }

    private void updateWeightChange(List<HealthEntry> recentEntries) {
        if (recentEntries.size() < 2 || recentEntries.get(0).getWeightKg() == null || recentEntries.get(1).getWeightKg() == null) {
            weightChangeLabel.setText("No previous data");
            weightChangeLabel.setStyle("-fx-fill: #6b7280;");
            return;
        }
        Double currentWeight = recentEntries.get(0).getWeightKg();
        Double previousWeight = recentEntries.get(1).getWeightKg();
        double change = currentWeight - previousWeight;
        weightChangeLabel.setText(String.format("%+.1f kg", change));
        weightChangeLabel.setStyle(change > 0 ? "-fx-fill: #ef4444;" : (change < 0 ? "-fx-fill: #16a34a;" : "-fx-fill: #6b7280;"));
    }

    private void populateHistory(List<HealthEntry> entries) {
        historyVBox.getChildren().clear();
        if (entries.isEmpty()) {
            historyVBox.getChildren().add(new Label("No recent history."));
            return;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
        for (HealthEntry entry : entries) {
            Label dateLabel = new Label(entry.getEntryDate().format(formatter));
            dateLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #374151;");

            String weightStr = entry.getWeightKg() != null ? entry.getWeightKg() + " kg" : "N/A";
            String sleepStr = entry.getHoursSlept() != null ? entry.getHoursSlept() + " h" : "N/A";
            String bpStr = entry.getBloodPressure() != null && !entry.getBloodPressure().isEmpty() ? entry.getBloodPressure() : "N/A";
            Label detailsLabel = new Label(String.format("Wt: %s • Sl: %s • BP: %s", weightStr, sleepStr, bpStr));
            detailsLabel.setStyle("-fx-text-fill: #6b7280;");

            VBox entryBox = new VBox(dateLabel, detailsLabel);
            entryBox.setStyle("-fx-background-color: white; -fx-padding: 10; -fx-background-radius: 8; -fx-border-color: #e5e7eb; -fx-border-radius: 8;");
            historyVBox.getChildren().add(entryBox);
        }
    }

    private void showStatusMessage(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().setAll(isError ? "status-label-error" : "status-label-success");
        PauseTransition visiblePause = new PauseTransition(Duration.seconds(4));
        visiblePause.setOnFinished(event -> statusLabel.setText(""));
        visiblePause.play();
    }

    private void clearForm() {
        weightField.clear();
        bloodPressureField.clear();
        sleepField.clear();
    }
}