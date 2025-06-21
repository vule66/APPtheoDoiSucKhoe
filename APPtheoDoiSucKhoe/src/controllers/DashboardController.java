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
    @FXML private VBox dataBox;
    @FXML private VBox noDataBox;

    private final HealthEntryService entryService = new HealthEntryService();

    // Trong DashboardController.java
    public void setCurrentUser(User user) {
        welcomeText.setText("Welcome back, " + user.getFullName() + "!");
        // Sửa dòng này:
        loadLatestHealthData(user.getId()); // Thay .getUserId() bằng .getId()
    }

    private void loadLatestHealthData(int userId) {
        try {
            Optional<HealthEntry> latestEntryOpt = entryService.findLatestEntryByUserId(userId);

            if (latestEntryOpt.isPresent()) {
                // Nếu có dữ liệu, hiển thị hộp dữ liệu và ẩn hộp "No Data"
                dataBox.setVisible(true);
                dataBox.setManaged(true);
                noDataBox.setVisible(false);
                noDataBox.setManaged(false);

                populateDashboard(latestEntryOpt.get());
            } else {
                // Nếu không có dữ liệu, làm ngược lại
                dataBox.setVisible(false);
                dataBox.setManaged(false);
                noDataBox.setVisible(true);
                noDataBox.setManaged(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // Xử lý lỗi nếu không thể truy vấn database
            dataBox.setVisible(false);
            noDataBox.setVisible(true);
            noDataBox.getChildren().setAll(new Label("Error: Could not connect to the database."));
        }
    }

    private void populateDashboard(HealthEntry entry) {
        // Định dạng ngày tháng cho đẹp hơn
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
        lastEntryDateText.setText("Last entry on: " + entry.getEntryDate().format(formatter));

        // Hiển thị dữ liệu, có kiểm tra null để tránh lỗi
        weightText.setText(entry.getWeightKg() != null ? String.format("%.1f kg", entry.getWeightKg()) : "- kg");
        bloodPressureText.setText(entry.getBloodPressure() != null && !entry.getBloodPressure().isEmpty() ? entry.getBloodPressure() : "- / -");
        sleepText.setText(entry.getHoursSlept() != null ? String.format("%.1f hrs", entry.getHoursSlept()) : "- hrs");
    }
}