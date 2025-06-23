package controllers;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import models.User;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class HealthTrackerController implements Initializable {


    @FXML private StackPane mainContentPane;
    @FXML private Label userNameLabel;
    @FXML private Label userInitialsLabel;
    @FXML private Label dateTimeLabel;
    @FXML private Button dashboardBtn;
    @FXML private Button dataEntryBtn;
    @FXML private Button analyticsBtn;
    @FXML private Button goalsBtn;
    @FXML private Button historyBtn;
    @FXML private Button logoutBtn;
    private Button currentActiveButton;
    private User currentUser;
    private Timeline clockTimeline;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        startClock();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (this.currentUser != null) {
            updateUserDisplay();
            handleMenuClick(new ActionEvent(dashboardBtn, dashboardBtn));
        } else {
            mainContentPane.getChildren().setAll(new Label("Critical Error: Could not load user data."));
        }
    }

    @FXML
    private void handleMenuClick(ActionEvent event) {
        Button clickedButton = (Button) event.getSource();

        if (clickedButton == currentActiveButton && mainContentPane.getChildren().size() > 0) {
            return;
        }

        String fxmlFileToLoad = "";
        if (clickedButton == dashboardBtn) {
            fxmlFileToLoad = "/application/DashboardView.fxml";
        } else if (clickedButton == dataEntryBtn) {

            fxmlFileToLoad = "/application/DataEntryView.fxml";
        } else if (clickedButton == analyticsBtn) {
            fxmlFileToLoad = "/application/AnalyticsView.fxml";
        } else if (clickedButton == goalsBtn) {
            fxmlFileToLoad = "/application/GoalsView.fxml";
        }

        if (!fxmlFileToLoad.isEmpty()) {
            loadView(fxmlFileToLoad);
            updateActiveButton(clickedButton);
        } else {
            mainContentPane.getChildren().setAll(new Label("Feature coming soon for: " + clickedButton.getText()));
            updateActiveButton(clickedButton);
        }
    }

    private void loadView(String fxmlPath) {
        if (currentUser == null) {
            System.err.println("CRITICAL: Attempted to load view without a user. Aborting.");
            return;
        }
        try {
            URL resourceUrl = getClass().getResource(fxmlPath);
            if (resourceUrl == null) {
                System.err.println("FATAL: FXML file not found at path: " + fxmlPath);
                mainContentPane.getChildren().setAll(new Label("Error: View file not found."));
                return;
            }

            FXMLLoader loader = new FXMLLoader(resourceUrl);
            Node view = loader.load();

            Object controller = loader.getController();
            if (controller instanceof DashboardController) {
                ((DashboardController) controller).setCurrentUser(this.currentUser);
            } else if (controller instanceof DataEntryController) {
                ((DataEntryController) controller).setCurrentUser(this.currentUser);
            }
            else if (controller instanceof AnalyticsController) {
                ((AnalyticsController) controller).setCurrentUser(this.currentUser);
            }
            else if (controller instanceof GoalsController) {
                ((GoalsController) controller).setCurrentUser(this.currentUser);
            }
            else if (controller instanceof GoalDialogController) {
                ((GoalDialogController) controller).setCurrentUser(this.currentUser);
            }

            mainContentPane.getChildren().setAll(view);

        } catch (IOException e) {
            e.printStackTrace();
            mainContentPane.getChildren().setAll(new Label("Error loading view: " + fxmlPath));
        }
    }

    private void updateActiveButton(Button newActiveButton) {
        if (currentActiveButton != null) {
            currentActiveButton.getStyleClass().remove("is-active");
        }
        newActiveButton.getStyleClass().add("is-active");
        currentActiveButton = newActiveButton;
    }


    private void updateUserDisplay() {
        if (currentUser == null) return;
        String fullName = currentUser.getFullName();
        if (fullName != null && !fullName.trim().isEmpty()) {
            userNameLabel.setText(fullName);
            String[] names = fullName.trim().split("\\s+");
            String initials = "";
            if (names.length > 0) initials += names[0].charAt(0);
            if (names.length > 1) initials += names[names.length - 1].charAt(0);
            userInitialsLabel.setText(initials.toUpperCase());
        } else {
            String username = currentUser.getUsername();
            userNameLabel.setText(username);
            userInitialsLabel.setText(username.length() > 1 ? username.substring(0, 2).toUpperCase() : username.toUpperCase());
        }
    }

    private void startClock() {
        clockTimeline = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy • HH:mm:ss");
            dateTimeLabel.setText(LocalDateTime.now().format(formatter));
        }), new KeyFrame(Duration.seconds(1)));
        clockTimeline.setCycleCount(Animation.INDEFINITE);
        clockTimeline.play();
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            if (clockTimeline != null) clockTimeline.stop();
            Stage stage = (Stage) logoutBtn.getScene().getWindow();
            // Tải màn hình đăng nhập
            Parent loginRoot = FXMLLoader.load(getClass().getResource("/application/Login.fxml"));
            Scene loginScene = new Scene(loginRoot);
            stage.setScene(loginScene);
            stage.setTitle("Health Tracker Pro - Login");
            stage.setMaximized(false);
            stage.sizeToScene();
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error loading login view: " + e.getMessage());
        }
    }
}