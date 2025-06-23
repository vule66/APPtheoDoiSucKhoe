package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import models.HealthEntry;
import models.User;
import services.HealthEntryService;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.DoubleSummaryStatistics;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.ResourceBundle;

public class AnalyticsController implements Initializable {

    @FXML private ComboBox<String> timePeriodComboBox;
    @FXML private Label startDateLabel;
    @FXML private Label endDateLabel;

    // Stats Labels
    @FXML private Label avgWeightLabel;
    @FXML private Label minWeightLabel;
    @FXML private Label maxWeightLabel;
    @FXML private Label avgBpLabel;
    @FXML private Text bpCategoryText;
    @FXML private Label avgHrLabel;
    @FXML private Label minHrLabel;
    @FXML private Label maxHrLabel;
    @FXML private Label avgStepsLabel;
    @FXML private Label stepsGoalLabel;
    @FXML private Label avgSleepLabel;
    @FXML private Label sleepQualityLabel;
    @FXML private Label avgCaloriesLabel;
    @FXML private Label caloriesTrendLabel;
    @FXML private Label avgWaterLabel;
    @FXML private Label waterGoalLabel;
    @FXML private Label dataCompletenessLabel;
    @FXML private Label daysLoggedLabel;

    // Charts
    @FXML private LineChart<String, Number> weightChart;
    @FXML private LineChart<String, Number> bpChart;
    @FXML private LineChart<String, Number> heartRateChart;
    @FXML private BarChart<String, Number> stepsChart;
    @FXML private LineChart<String, Number> sleepChart;
    @FXML private BarChart<String, Number> caloriesChart;
    @FXML private LineChart<String, Number> waterChart;

    @FXML private VBox noDataBox;

    private HealthEntryService entryService;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM");
    private List<HealthEntry> healthData;
    private LocalDate startDate;
    private LocalDate endDate;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        entryService = new HealthEntryService();

        setupTimePeriodComboBox();

        // Mặc định là 30 ngày gần nhất
        timePeriodComboBox.getSelectionModel().select("Last 30 Days");

        loadData();
    }

    private void setupTimePeriodComboBox() {
        ObservableList<String> options = FXCollections.observableArrayList(
                "Last 7 Days",
                "Last 30 Days",
                "Last 90 Days",
                "Last 180 Days",
                "Last 365 Days"
        );
        timePeriodComboBox.setItems(options);

        // Xử lý sự kiện khi người dùng chọn khoảng thời gian
        timePeriodComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                calculateDateRange(newVal);
                loadData();
            }
        });
    }

    private void calculateDateRange(String period) {
        endDate = LocalDate.now();

        switch (period) {
            case "Last 7 Days":
                startDate = endDate.minusDays(6); // 7 days including today
                break;
            case "Last 30 Days":
                startDate = endDate.minusDays(29); // 30 days including today
                break;
            case "Last 90 Days":
                startDate = endDate.minusDays(89); // 90 days including today
                break;
            case "Last 180 Days":
                startDate = endDate.minusDays(179); // 180 days including today
                break;
            case "Last 365 Days":
                startDate = endDate.minusDays(364); // 365 days including today
                break;
            default:
                startDate = endDate.minusDays(29); // Default to 30 days
        }

        // Cập nhật nhãn ngày
        startDateLabel.setText(startDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
        endDateLabel.setText(endDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
    }

    @FXML
    private void handleRefresh() {
        loadData();
    }
    private User currentUser;

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    private void loadData() {
        if (currentUser == null) return;

        try {
            healthData = entryService.findEntriesByUserIdAndDateRange(
                    currentUser.getId(), startDate, endDate);

            if (healthData.isEmpty()) {
                showNoDataMessage(true);
            } else {
                showNoDataMessage(false);
                calculateStatistics();
                updateCharts();
            }
        } catch (SQLException e) {
            System.err.println("Error loading health data: " + e.getMessage());
            e.printStackTrace();
            showNoDataMessage(true);
        }

    }

    private void showNoDataMessage(boolean show) {
        noDataBox.setVisible(show);
        noDataBox.setManaged(show);
    }

    private void calculateStatistics() {
        // Tính toán các thống kê

        // Weight Statistics
        DoubleSummaryStatistics weightStats = healthData.stream()
                .filter(entry -> entry.getWeight() != null)
                .mapToDouble(HealthEntry::getWeight)
                .summaryStatistics();

        if (weightStats.getCount() > 0) {
            avgWeightLabel.setText(String.format("%.1f", weightStats.getAverage()));
            minWeightLabel.setText(String.format("%.1f", weightStats.getMin()));
            maxWeightLabel.setText(String.format("%.1f", weightStats.getMax()));
        } else {
            avgWeightLabel.setText("--");
            minWeightLabel.setText("--");
            maxWeightLabel.setText("--");
        }

        // Blood Pressure
        double avgSystolic = healthData.stream()
                .filter(entry -> entry.getSystolicBp() != null)
                .mapToInt(HealthEntry::getSystolicBp)
                .average()
                .orElse(0);

        double avgDiastolic = healthData.stream()
                .filter(entry -> entry.getDiastolicBp() != null)
                .mapToInt(HealthEntry::getDiastolicBp)
                .average()
                .orElse(0);

        if (avgSystolic > 0 && avgDiastolic > 0) {
            avgBpLabel.setText(String.format("%.0f/%.0f", avgSystolic, avgDiastolic));

            // Categorize blood pressure
            if (avgSystolic < 120 && avgDiastolic < 80) {
                bpCategoryText.setText("Normal");
                bpCategoryText.setStyle("-fx-fill: #10b981;");
            } else if (avgSystolic < 130 && avgDiastolic < 80) {
                bpCategoryText.setText("Elevated");
                bpCategoryText.setStyle("-fx-fill: #f59e0b;");
            } else if (avgSystolic < 140 || avgDiastolic < 90) {
                bpCategoryText.setText("Stage 1 Hypertension");
                bpCategoryText.setStyle("-fx-fill: #ef4444;");
            } else {
                bpCategoryText.setText("Stage 2 Hypertension");
                bpCategoryText.setStyle("-fx-fill: #b91c1c;");
            }
        } else {
            avgBpLabel.setText("--/--");
            bpCategoryText.setText("Not enough data");
            bpCategoryText.setStyle("-fx-fill: #64748b;");
        }

        // Heart Rate
        IntSummaryStatistics hrStats = healthData.stream()
                .filter(entry -> entry.getHeartRate() != null)
                .mapToInt(HealthEntry::getHeartRate)
                .summaryStatistics();

        if (hrStats.getCount() > 0) {
            avgHrLabel.setText(String.format("%.0f", hrStats.getAverage()));
            minHrLabel.setText(String.valueOf(hrStats.getMin()));
            maxHrLabel.setText(String.valueOf(hrStats.getMax()));
        } else {
            avgHrLabel.setText("--");
            minHrLabel.setText("--");
            maxHrLabel.setText("--");
        }

        // Steps
        double avgSteps = healthData.stream()
                .filter(entry -> entry.getSteps() != null)
                .mapToInt(HealthEntry::getSteps)
                .average()
                .orElse(0);

        if (avgSteps > 0) {
            avgStepsLabel.setText(String.format("%,d", (int)avgSteps));
            int stepsGoalPercent = (int)((avgSteps / 10000) * 100);
            stepsGoalLabel.setText(stepsGoalPercent + "% of 10,000 goal");
        } else {
            avgStepsLabel.setText("--");
            stepsGoalLabel.setText("No data available");
        }

        // Sleep
        DoubleSummaryStatistics sleepStats = healthData.stream()
                .filter(entry -> entry.getSleepHours() != null)
                .mapToDouble(HealthEntry::getSleepHours)
                .summaryStatistics();

        if (sleepStats.getCount() > 0) {
            double avgSleep = sleepStats.getAverage();
            avgSleepLabel.setText(String.format("%.1f", avgSleep));

            // Sleep quality assessment
            if (avgSleep >= 7 && avgSleep <= 9) {
                sleepQualityLabel.setText("Good quality");
                sleepQualityLabel.setStyle("-fx-text-fill: #10b981;");
            } else if (avgSleep >= 6 && avgSleep < 7) {
                sleepQualityLabel.setText("Adequate");
                sleepQualityLabel.setStyle("-fx-text-fill: #f59e0b;");
            } else {
                sleepQualityLabel.setText("Needs improvement");
                sleepQualityLabel.setStyle("-fx-text-fill: #ef4444;");
            }
        } else {
            avgSleepLabel.setText("--");
            sleepQualityLabel.setText("No data available");
            sleepQualityLabel.setStyle("-fx-text-fill: #64748b;");
        }

        // Calories
        double avgCalories = healthData.stream()
                .filter(entry -> entry.getCalories() != null)
                .mapToInt(HealthEntry::getCalories)
                .average()
                .orElse(0);

        if (avgCalories > 0) {
            avgCaloriesLabel.setText(String.format("%,d", (int)avgCalories));

            // Calories trend
            if (avgCalories < 1500) {
                caloriesTrendLabel.setText("Deficit - Weight Loss");
                caloriesTrendLabel.setStyle("-fx-text-fill: #10b981;");
            } else if (avgCalories >= 1500 && avgCalories <= 2500) {
                caloriesTrendLabel.setText("Maintenance");
                caloriesTrendLabel.setStyle("-fx-text-fill: #0ea5e9;");
            } else {
                caloriesTrendLabel.setText("Surplus - Weight Gain");
                caloriesTrendLabel.setStyle("-fx-text-fill: #f59e0b;");
            }
        } else {
            avgCaloriesLabel.setText("--");
            caloriesTrendLabel.setText("No data available");
            caloriesTrendLabel.setStyle("-fx-text-fill: #64748b;");
        }

        // Water Intake
        DoubleSummaryStatistics waterStats = healthData.stream()
                .filter(entry -> entry.getWaterIntake() != null)
                .mapToDouble(HealthEntry::getWaterIntake)
                .summaryStatistics();

        if (waterStats.getCount() > 0) {
            double avgWater = waterStats.getAverage();
            avgWaterLabel.setText(String.format("%.1f", avgWater));

            int waterGoalPercent = (int)((avgWater / 2.5) * 100);
            waterGoalLabel.setText(waterGoalPercent + "% of 2.5L goal");
        } else {
            avgWaterLabel.setText("--");
            waterGoalLabel.setText("No data available");
        }

        // Data Completeness
        int daysBetween = (int) (endDate.toEpochDay() - startDate.toEpochDay() + 1);
        int daysLogged = (int) healthData.stream()
                .map(HealthEntry::getRecordDate)
                .distinct()
                .count();

        int completeness = (int)(((double)daysLogged / daysBetween) * 100);
        dataCompletenessLabel.setText(String.valueOf(completeness));
        daysLoggedLabel.setText(daysLogged + " of " + daysBetween + " days logged");
    }

    private void updateCharts() {
        // Cập nhật biểu đồ

        updateWeightChart();
        updateBloodPressureChart();
        updateHeartRateChart();
        updateStepsChart();
        updateSleepChart();
        updateCaloriesChart();
        updateWaterChart();
    }

    private void updateWeightChart() {
        weightChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Weight");

        healthData.stream()
                .filter(entry -> entry.getWeight() != null)
                .sorted((a, b) -> a.getRecordDate().compareTo(b.getRecordDate()))
                .forEach(entry -> {
                    series.getData().add(new XYChart.Data<>(
                            entry.getRecordDate().format(dateFormatter),
                            entry.getWeight()
                    ));
                });

        weightChart.getData().add(series);
    }

    private void updateBloodPressureChart() {
        bpChart.getData().clear();

        XYChart.Series<String, Number> systolicSeries = new XYChart.Series<>();
        systolicSeries.setName("Systolic");

        XYChart.Series<String, Number> diastolicSeries = new XYChart.Series<>();
        diastolicSeries.setName("Diastolic");

        healthData.stream()
                .filter(entry -> entry.getSystolicBp() != null && entry.getDiastolicBp() != null)
                .sorted((a, b) -> a.getRecordDate().compareTo(b.getRecordDate()))
                .forEach(entry -> {
                    String date = entry.getRecordDate().format(dateFormatter);
                    systolicSeries.getData().add(new XYChart.Data<>(date, entry.getSystolicBp()));
                    diastolicSeries.getData().add(new XYChart.Data<>(date, entry.getDiastolicBp()));
                });

        bpChart.getData().addAll(systolicSeries, diastolicSeries);
    }

    private void updateHeartRateChart() {
        heartRateChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Heart Rate");

        healthData.stream()
                .filter(entry -> entry.getHeartRate() != null)
                .sorted((a, b) -> a.getRecordDate().compareTo(b.getRecordDate()))
                .forEach(entry -> {
                    series.getData().add(new XYChart.Data<>(
                            entry.getRecordDate().format(dateFormatter),
                            entry.getHeartRate()
                    ));
                });

        heartRateChart.getData().add(series);
    }

    private void updateStepsChart() {
        stepsChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Steps");

        healthData.stream()
                .filter(entry -> entry.getSteps() != null)
                .sorted((a, b) -> a.getRecordDate().compareTo(b.getRecordDate()))
                .forEach(entry -> {
                    series.getData().add(new XYChart.Data<>(
                            entry.getRecordDate().format(dateFormatter),
                            entry.getSteps()
                    ));
                });

        stepsChart.getData().add(series);
    }

    private void updateSleepChart() {
        sleepChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Sleep");

        healthData.stream()
                .filter(entry -> entry.getSleepHours() != null)
                .sorted((a, b) -> a.getRecordDate().compareTo(b.getRecordDate()))
                .forEach(entry -> {
                    series.getData().add(new XYChart.Data<>(
                            entry.getRecordDate().format(dateFormatter),
                            entry.getSleepHours()
                    ));
                });

        sleepChart.getData().add(series);
    }

    private void updateCaloriesChart() {
        caloriesChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Calories");

        healthData.stream()
                .filter(entry -> entry.getCalories() != null)
                .sorted((a, b) -> a.getRecordDate().compareTo(b.getRecordDate()))
                .forEach(entry -> {
                    series.getData().add(new XYChart.Data<>(
                            entry.getRecordDate().format(dateFormatter),
                            entry.getCalories()
                    ));
                });

        caloriesChart.getData().add(series);
    }

    private void updateWaterChart() {
        waterChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Water Intake");

        healthData.stream()
                .filter(entry -> entry.getWaterIntake() != null)
                .sorted((a, b) -> a.getRecordDate().compareTo(b.getRecordDate()))
                .forEach(entry -> {
                    series.getData().add(new XYChart.Data<>(
                            entry.getRecordDate().format(dateFormatter),
                            entry.getWaterIntake()
                    ));
                });

        waterChart.getData().add(series);
    }
}