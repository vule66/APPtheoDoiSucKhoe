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
import database.DatabaseManager;  // Thêm import này

import java.sql.Connection;        // Thêm 3 import này
import java.sql.ResultSet;
import java.sql.Statement;
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
    @FXML private TextField stepsField;
    @FXML private TextField heartRateField;
    @FXML private TextField caloriesField;
    @FXML private TextField waterIntakeField;

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

    // Chỉ giữ một phiên bản của debugDatabase()
    @FXML
    private void debugDatabase() {
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            // Kiểm tra bảng có tồn tại không
            Statement checkStmt = conn.createStatement();
            ResultSet rs = checkStmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='health_data'");
            boolean tableExists = rs.next();
            System.out.println("Table health_data exists: " + tableExists);
            rs.close();

            if (tableExists) {
                // Thử INSERT trực tiếp
                String sql = "INSERT INTO health_data (user_id, record_date, weight, systolic_bp, diastolic_bp, sleep_hours) " +
                        "VALUES (1, '2025-06-21', 70.5, 120, 80, 7.5)";
                Statement stmt = conn.createStatement();
                int result = stmt.executeUpdate(sql);
                System.out.println("Manual insert result: " + result + " rows affected");
            } else {
                System.err.println("ERROR: Table health_data does not exist!");
            }
        } catch (SQLException e) {
            System.err.println("Debug INSERT error: " + e.getMessage());
            e.printStackTrace();
        }
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

        Integer systolicBp = null;
        Integer diastolicBp = null;
        if (bloodPressure != null && !bloodPressure.isEmpty()) {
            String[] parts = bloodPressure.split("/");
            if (parts.length == 2) {
                try {
                    systolicBp = Integer.parseInt(parts[0].trim());
                    diastolicBp = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException e) {
                    showStatusMessage("Error: Invalid blood pressure format. Use systolic/diastolic format.", true);
                    return;
                }
            } else {
                showStatusMessage("Error: Invalid blood pressure format. Use systolic/diastolic format.", true);
                return;
            }
        }

        // Thêm phần xử lý các trường mới
        Integer steps = null;
        if (stepsField != null && !stepsField.getText().trim().isEmpty()) {
            try {
                steps = Integer.parseInt(stepsField.getText().trim());
            } catch (NumberFormatException e) {
                showStatusMessage("Error: Invalid number for Steps.", true);
                return;
            }
        }

        Integer heartRate = null;
        if (heartRateField != null && !heartRateField.getText().trim().isEmpty()) {
            try {
                heartRate = Integer.parseInt(heartRateField.getText().trim());
            } catch (NumberFormatException e) {
                showStatusMessage("Error: Invalid number for Heart Rate.", true);
                return;
            }
        }

        Integer calories = null;
        if (caloriesField != null && !caloriesField.getText().trim().isEmpty()) {
            try {
                calories = Integer.parseInt(caloriesField.getText().trim());
            } catch (NumberFormatException e) {
                showStatusMessage("Error: Invalid number for Calories.", true);
                return;
            }
        }

        Double waterIntake = null;
        if (waterIntakeField != null && !waterIntakeField.getText().trim().isEmpty()) {
            try {
                waterIntake = Double.parseDouble(waterIntakeField.getText().trim().replace(',', '.'));
            } catch (NumberFormatException e) {
                showStatusMessage("Error: Invalid number for Water Intake.", true);
                return;
            }
        }

        // Tạo đối tượng HealthEntry (CHỈ TẠO MỘT LẦN)
        // XÓA dòng tạo entry ở đây nếu đã có

        // Sử dụng constructor mới với các trường cơ bản
        HealthEntry entry = new HealthEntry(currentUser.getId(), entryDate);
        entry.setWeight(weight);
        entry.setSystolicBp(systolicBp);
        entry.setDiastolicBp(diastolicBp);
        entry.setSleepHours(sleep);

        // Set các giá trị mới
        entry.setSteps(steps);
        entry.setHeartRate(heartRate);
        entry.setCalories(calories);
        entry.setWaterIntake(waterIntake);

        System.out.println("DEBUG - Entry created: " + entry);

        try {
            // Gọi phương thức lưu thực sự
            entryService.saveOrUpdateEntry(entry);
            showStatusMessage("Entry saved successfully!", false);
            updateSidePanel();
        } catch (SQLException e) {
            showStatusMessage("Database error: " + e.getMessage(), true);
            System.err.println("SQL ERROR: " + e.getMessage());
            System.err.println("SQL State: " + e.getSQLState());
            System.err.println("Error Code: " + e.getErrorCode());
            e.printStackTrace();
        }
    }


    // Phần code còn lại giữ nguyên
    private void loadEntryForDate(LocalDate date) {
        if (currentUser == null) return;
        try {
            Optional<HealthEntry> entryOpt = entryService.findEntryByUserIdAndDate(currentUser.getId(), date);
            if (entryOpt.isPresent()) {
                HealthEntry entry = entryOpt.get();
                weightField.setText(entry.getWeight() != null ? String.valueOf(entry.getWeight()) : "");
                bloodPressureField.setText(entry.getBloodPressure() != null ? entry.getBloodPressure() : "");
                sleepField.setText(entry.getSleepHours() != null ? String.valueOf(entry.getSleepHours()) : "");

                // Điền các trường mới
                stepsField.setText(entry.getSteps() != null ? String.valueOf(entry.getSteps()) : "");
                heartRateField.setText(entry.getHeartRate() != null ? String.valueOf(entry.getHeartRate()) : "");
                caloriesField.setText(entry.getCalories() != null ? String.valueOf(entry.getCalories()) : "");
                waterIntakeField.setText(entry.getWaterIntake() != null ? String.valueOf(entry.getWaterIntake()) : "");
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
        if (currentUser.getHeight() <= 0 || recentEntries.isEmpty() || recentEntries.get(0).getWeight() == null) {
            bmiValueLabel.setText("--");
            bmiCategoryLabel.setText("Need height & weight");
            return;
        }
        Double latestWeight = recentEntries.get(0).getWeight();
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
        if (recentEntries.size() < 2 || recentEntries.get(0).getWeight() == null || recentEntries.get(1).getWeight() == null) {
            weightChangeLabel.setText("No previous data");
            weightChangeLabel.setStyle("-fx-fill: #6b7280;");
            return;
        }
        Double currentWeight = recentEntries.get(0).getWeight();
        Double previousWeight = recentEntries.get(1).getWeight();
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

            String weightStr = entry.getWeight() != null ? entry.getWeight() + " kg" : "N/A";
            String sleepStr = entry.getSleepHours() != null ? entry.getSleepHours() + " h" : "N/A";
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
        stepsField.clear();
        heartRateField.clear();
        caloriesField.clear();
        waterIntakeField.clear();
    }

}