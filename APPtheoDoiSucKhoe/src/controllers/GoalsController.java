package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import models.Goal;
import models.User;
import services.GoalService;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controller cho màn hình Goals - quản lý mục tiêu sức khỏe
 */
public class GoalsController implements Initializable {

    // --- FXML Elements ---
    @FXML private VBox mainContainer;
    @FXML private Label titleLabel;
    @FXML private Button addGoalBtn;
    @FXML private Button refreshBtn;
    @FXML private CheckBox showCompletedCheckBox;

    // Statistics section
    @FXML private Label totalGoalsLabel;
    @FXML private Label activeGoalsLabel;
    @FXML private Label completedGoalsLabel;
    @FXML private Label overdueGoalsLabel;
    @FXML private ProgressBar completionRateBar;
    @FXML private Label completionRateLabel;

    // Goals table
    @FXML private TableView<Goal> goalsTable;
    @FXML private TableColumn<Goal, String> typeColumn;
    @FXML private TableColumn<Goal, String> descriptionColumn;
    @FXML private TableColumn<Goal, String> progressColumn;
    @FXML private TableColumn<Goal, String> dueDateColumn;
    @FXML private TableColumn<Goal, String> statusColumn;
    @FXML private TableColumn<Goal, String> priorityColumn;
    @FXML private TableColumn<Goal, String> actionsColumn;

    // --- State Variables ---
    private User currentUser;
    private GoalService goalService;
    private ObservableList<Goal> goalsList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        goalService = new GoalService();
        goalsList = FXCollections.observableArrayList();

        setupTable();
        setupEventHandlers();

        // Set table items
        goalsTable.setItems(goalsList);
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (this.currentUser != null) {
            loadGoals();
            updateStatistics();
        }
    }

    private void setupTable() {
        // Setup table properties
        goalsTable.setRowFactory(tv -> {
            TableRow<Goal> row = new TableRow<>();
            row.itemProperty().addListener((obs, oldGoal, newGoal) -> {
                if (newGoal == null) {
                    row.setStyle("");
                } else if (newGoal.isAchieved()) {
                    row.setStyle("-fx-background-color: #d4edda;");
                } else if (newGoal.isOverdue()) {
                    row.setStyle("-fx-background-color: #f8d7da;");
                } else if (newGoal.getDaysRemaining() <= 7) {
                    row.setStyle("-fx-background-color: #fff3cd;");
                }
            });
            return row;
        });

        // Setup table columns
        typeColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getGoalType().getDisplayName()));

        descriptionColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getGoalDescription()));

        progressColumn.setCellValueFactory(data -> {
            Goal goal = data.getValue();
            String progressText = String.format("%.1f%% (%.1f/%.1f %s)",
                    goal.getProgressPercentage() * 100,
                    goal.getCurrentValue(),
                    goal.getTargetValue(),
                    goal.getTargetUnit());
            return new javafx.beans.property.SimpleStringProperty(progressText);
        });

        dueDateColumn.setCellValueFactory(data -> {
            LocalDate targetDate = data.getValue().getTargetDate();
            String dateStr = targetDate != null ? targetDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "N/A";
            return new javafx.beans.property.SimpleStringProperty(dateStr);
        });

        statusColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getStatus()));

        priorityColumn.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getPriority().getDisplayName()));

        // Setup actions column with buttons
        actionsColumn.setCellFactory(new Callback<TableColumn<Goal, String>, TableCell<Goal, String>>() {
            @Override
            public TableCell<Goal, String> call(TableColumn<Goal, String> param) {
                return new TableCell<Goal, String>() {
                    private final Button editBtn = new Button("✏️");
                    private final Button deleteBtn = new Button("🗑️");
                    private final Button updateBtn = new Button("📊");
                    private final HBox buttonsBox = new HBox(5);

                    {
                        // Style buttons
                        editBtn.getStyleClass().add("action-button-edit");
                        deleteBtn.getStyleClass().add("action-button-delete");
                        updateBtn.getStyleClass().add("action-button-update");

                        editBtn.setTooltip(new Tooltip("Edit Goal"));
                        deleteBtn.setTooltip(new Tooltip("Delete Goal"));
                        updateBtn.setTooltip(new Tooltip("Update Progress"));

                        // Button actions
                        editBtn.setOnAction(event -> {
                            Goal goal = getTableView().getItems().get(getIndex());
                            handleEditGoal(goal);
                        });

                        deleteBtn.setOnAction(event -> {
                            Goal goal = getTableView().getItems().get(getIndex());
                            handleDeleteGoal(goal);
                        });

                        updateBtn.setOnAction(event -> {
                            Goal goal = getTableView().getItems().get(getIndex());
                            handleUpdateProgress(goal);
                        });

                        buttonsBox.getChildren().addAll(editBtn, updateBtn, deleteBtn);
                        buttonsBox.setAlignment(Pos.CENTER);
                    }

                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(buttonsBox);
                        }
                    }
                };
            }
        });
    }

    private void setupEventHandlers() {
        addGoalBtn.setOnAction(this::handleAddGoal);
        refreshBtn.setOnAction(this::handleRefresh);
        showCompletedCheckBox.setOnAction(this::handleShowCompletedChange);
    }

    private void loadGoals() {
        if (currentUser == null) return;

        boolean includeCompleted = showCompletedCheckBox.isSelected();
        List<Goal> goals = goalService.getUserGoals(currentUser, includeCompleted);

        goalsList.clear();
        goalsList.addAll(goals);
    }

    private void updateStatistics() {
        if (currentUser == null) return;

        GoalService.GoalStatistics stats = goalService.getGoalStatistics(currentUser);

        totalGoalsLabel.setText(String.valueOf(stats.getTotalGoals()));
        activeGoalsLabel.setText(String.valueOf(stats.getActiveGoals()));
        completedGoalsLabel.setText(String.valueOf(stats.getCompletedGoals()));
        overdueGoalsLabel.setText(String.valueOf(stats.getOverdueGoals()));

        double completionRate = stats.getCompletionRate() / 100.0;
        completionRateBar.setProgress(completionRate);
        completionRateLabel.setText(String.format("%.1f%%", stats.getCompletionRate()));
    }

    @FXML
    private void handleAddGoal(ActionEvent event) {
        showGoalDialog(null);
    }

    @FXML
    private void handleRefresh(ActionEvent event) {
        // Update progress first
        if (currentUser != null) {
            goalService.updateGoalProgress(currentUser);
        }

        loadGoals();
        updateStatistics();

        showSuccessAlert("Data refreshed successfully!");
    }

    @FXML
    private void handleShowCompletedChange(ActionEvent event) {
        loadGoals();
    }

    private void handleEditGoal(Goal goal) {
        showGoalDialog(goal);
    }

    private void handleDeleteGoal(Goal goal) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText("Delete Goal");
        alert.setContentText("Are you sure you want to delete this goal: \"" + goal.getGoalDescription() + "\"?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (goalService.deleteGoal(goal.getId())) {
                loadGoals();
                updateStatistics();
                showSuccessAlert("Goal deleted successfully!");
            } else {
                showErrorAlert("Failed to delete goal!");
            }
        }
    }

    private void handleUpdateProgress(Goal goal) {
        TextInputDialog dialog = new TextInputDialog(String.valueOf(goal.getCurrentValue()));
        dialog.setTitle("Update Progress");
        dialog.setHeaderText("Update Goal Progress");
        dialog.setContentText("Enter current value (" + goal.getTargetUnit() + "):");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(value -> {
            try {
                double newValue = Double.parseDouble(value);
                goal.updateProgress(newValue);

                if (goalService.updateGoal(goal)) {
                    loadGoals();
                    updateStatistics();

                    if (goal.isAchieved()) {
                        showSuccessAlert("🎉 Congratulations! Goal achieved: " + goal.getGoalDescription());
                    } else {
                        showSuccessAlert("Progress updated successfully!");
                    }
                } else {
                    showErrorAlert("Failed to update progress!");
                }
            } catch (NumberFormatException e) {
                showErrorAlert("Invalid number format!");
            }
        });
    }

    private void showGoalDialog(Goal goal) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/Goaldialog.fxml"));
            Parent root = loader.load();

            GoalDialogController controller = loader.getController();
            controller.setCurrentUser(currentUser);

            if (goal != null) {
                controller.setGoal(goal); // Edit mode
            }

            Stage dialogStage = new Stage();
            dialogStage.setTitle(goal == null ? "Add New Goal" : "Edit Goal");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(addGoalBtn.getScene().getWindow());
            dialogStage.setScene(new Scene(root));
            dialogStage.setResizable(false);

            controller.setDialogStage(dialogStage);

            dialogStage.showAndWait();

            // Refresh if goal was saved
            if (controller.isGoalSaved()) {
                loadGoals();
                updateStatistics();
            }

        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Failed to open goal dialog!");
        }
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