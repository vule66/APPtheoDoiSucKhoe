package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import models.HealthEntry;
import models.User;
import services.HealthEntryService;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class DashboardController {

    @FXML private Text welcomeText;
    @FXML private Label lastEntryDateText;
    @FXML private Label weightText;
    @FXML private Label bloodPressureText;
    @FXML private Label sleepText;
    @FXML private Label stepsText;
    @FXML private Label caloriesText;
    @FXML private Label heartRateText;
    @FXML private Label waterIntakeText;
    @FXML private VBox dataBox;
    @FXML private VBox noDataBox;

    private final HealthEntryService entryService = new HealthEntryService();

    public void setCurrentUser(User user) {
        welcomeText.setText("Welcome back, " + user.getFullName() + "!");
        loadLatestHealthData(user.getId());
    }

    private void loadLatestHealthData(int userId) {
        try {
            Optional<HealthEntry> latestEntryOpt = entryService.findLatestEntryByUserId(userId);

            if (latestEntryOpt.isPresent()) {
                dataBox.setVisible(true);
                dataBox.setManaged(true);
                noDataBox.setVisible(false);
                noDataBox.setManaged(false);

                populateDashboard(latestEntryOpt.get());
            } else {
                dataBox.setVisible(false);
                dataBox.setManaged(false);
                noDataBox.setVisible(true);
                noDataBox.setManaged(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            dataBox.setVisible(false);
            noDataBox.setVisible(true);
            noDataBox.getChildren().setAll(new Label("Error: Could not connect to the database."));
        }
    }

    private void populateDashboard(HealthEntry entry) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
        lastEntryDateText.setText("Last entry on: " + entry.getRecordDate().format(formatter));

        weightText.setText(entry.getWeight() != null ? String.format("%.1f kg", entry.getWeight()) : "- kg");
        bloodPressureText.setText(entry.getBloodPressure() != null && !entry.getBloodPressure().isEmpty() ? entry.getBloodPressure() : "- / -");
        sleepText.setText(entry.getSleepHours() != null ? String.format("%.1f hrs", entry.getSleepHours()) : "- hrs");
        // Bổ sung các dòng này:
        stepsText.setText(entry.getSteps() != null ? String.valueOf(entry.getSteps()) : "-");
        heartRateText.setText(entry.getHeartRate() != null ? String.valueOf(entry.getHeartRate()) : "-");
        caloriesText.setText(entry.getCalories() != null ? String.valueOf(entry.getCalories()) : "-");
        waterIntakeText.setText(entry.getWaterIntake() != null ? String.format("%.2f", entry.getWaterIntake()) : "-");
    }
}