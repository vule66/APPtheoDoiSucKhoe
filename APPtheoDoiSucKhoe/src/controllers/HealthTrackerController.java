package controllers;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import models.User;
import services.HealthDataService;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class HealthTrackerController implements Initializable {

    // Header Elements
    @FXML private Label userNameLabel;
    @FXML private Label dateTimeLabel;

    // Navigation Buttons
    @FXML private Button dashboardBtn;
    @FXML private Button dataEntryBtn;
    @FXML private Button analyticsBtn;
    @FXML private Button goalsBtn;
    @FXML private Button historyBtn;
    @FXML private Button settingsBtn;
    @FXML private Button logoutBtn;

    // Content Panels
    @FXML private ScrollPane dashboardContent;
    @FXML private ScrollPane dataEntryContent;

    // Dashboard Elements
    @FXML private Label stepsLabel;
    @FXML private Label heartRateLabel;
    @FXML private Label caloriesLabel;
    @FXML private Label sleepLabel;
    @FXML private Label waterLabel;
    @FXML private Label weightLabel;

    // Services
    private HealthDataService healthDataService;
    private Timeline clockTimeline;

    // Current user - changed to User object
    private User currentUser;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        healthDataService = new HealthDataService();

        setupUserInterface();
        setupNavigation();
        setupButtonEffects();
        startClock();
    }

    private void setupUserInterface() {
        updateDateTime();

        // Set initial active state
        if (dashboardBtn != null) {
            setActiveNavButton(dashboardBtn);
        }
        if (dashboardContent != null) {
            showContent(dashboardContent);
        }
    }

    private void setupNavigation() {
        if (dashboardBtn != null) dashboardBtn.setOnAction(e -> navigateTo("dashboard"));
        if (dataEntryBtn != null) dataEntryBtn.setOnAction(e -> navigateTo("dataEntry"));
        if (analyticsBtn != null) analyticsBtn.setOnAction(e -> navigateTo("analytics"));
        if (goalsBtn != null) goalsBtn.setOnAction(e -> navigateTo("goals"));
        if (historyBtn != null) historyBtn.setOnAction(e -> navigateTo("history"));
        if (settingsBtn != null) settingsBtn.setOnAction(e -> navigateTo("settings"));
        if (logoutBtn != null) logoutBtn.setOnAction(this::handleLogout);
    }

    private void navigateTo(String section) {
        // Reset all nav buttons
        resetNavButtons();

        switch (section) {
            case "dashboard":
                if (dashboardBtn != null) setActiveNavButton(dashboardBtn);
                if (dashboardContent != null) showContent(dashboardContent);
                loadDashboardData();
                break;
            case "dataEntry":
                if (dataEntryBtn != null) setActiveNavButton(dataEntryBtn);
                if (dataEntryContent != null) showContent(dataEntryContent);
                break;
            case "analytics":
                if (analyticsBtn != null) setActiveNavButton(analyticsBtn);
                // TODO: Show analytics content
                break;
            case "goals":
                if (goalsBtn != null) setActiveNavButton(goalsBtn);
                // TODO: Show goals content
                break;
            case "history":
                if (historyBtn != null) setActiveNavButton(historyBtn);
                // TODO: Show history content
                break;
            case "settings":
                if (settingsBtn != null) setActiveNavButton(settingsBtn);
                // TODO: Show settings content
                break;
        }
    }

    private void resetNavButtons() {
        String inactiveStyle = "-fx-background-color: transparent; -fx-border-color: transparent; -fx-text-fill: rgba(255,255,255,0.8); -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 0;";

        if (dashboardBtn != null) dashboardBtn.setStyle(inactiveStyle);
        if (dataEntryBtn != null) dataEntryBtn.setStyle(inactiveStyle);
        if (analyticsBtn != null) analyticsBtn.setStyle(inactiveStyle);
        if (goalsBtn != null) goalsBtn.setStyle(inactiveStyle);
        if (historyBtn != null) historyBtn.setStyle(inactiveStyle);
    }

    private void setActiveNavButton(Button button) {
        String activeStyle = "-fx-background-color: rgba(16, 185, 129, 0.2); -fx-border-color: transparent; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 0;";
        button.setStyle(activeStyle);
    }

    private void showContent(ScrollPane content) {
        // Hide all content
        if (dashboardContent != null) dashboardContent.setVisible(false);
        if (dataEntryContent != null) dataEntryContent.setVisible(false);

        // Show selected content with fade effect
        content.setVisible(true);
        FadeTransition ft = new FadeTransition(Duration.millis(300), content);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();
    }

    private void setupButtonEffects() {
        // Add hover effects to navigation buttons
        if (dashboardBtn != null) addHoverEffect(dashboardBtn);
        if (dataEntryBtn != null) addHoverEffect(dataEntryBtn);
        if (analyticsBtn != null) addHoverEffect(analyticsBtn);
        if (goalsBtn != null) addHoverEffect(goalsBtn);
        if (historyBtn != null) addHoverEffect(historyBtn);
        if (settingsBtn != null) addHoverEffect(settingsBtn);
    }

    private void addHoverEffect(Button button) {
        button.setOnMouseEntered(e -> {
            if (!button.getStyle().contains("rgba(16, 185, 129, 0.2)")) {
                button.setStyle(button.getStyle() + "; -fx-background-color: rgba(255,255,255,0.1);");
            }
        });

        button.setOnMouseExited(e -> {
            if (!button.getStyle().contains("rgba(16, 185, 129, 0.2)")) {
                button.setStyle(button.getStyle().replace("; -fx-background-color: rgba(255,255,255,0.1);", ""));
            }
        });
    }

    private void startClock() {
        clockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateDateTime()));
        clockTimeline.setCycleCount(Animation.INDEFINITE);
        clockTimeline.play();
    }

    private void updateDateTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy • HH:mm");
        if (dateTimeLabel != null) {
            dateTimeLabel.setText(now.format(formatter));
        }
    }

    private void loadDashboardData() {
        if (currentUser == null) return;

        // Load current health data using user ID
        // For now, use sample data
        updateDashboardLabels();
    }

    private void updateDashboardLabels() {
        // Sample data - replace with real data from database
        if (stepsLabel != null) stepsLabel.setText("8,542");
        if (heartRateLabel != null) heartRateLabel.setText("72 BPM");
        if (caloriesLabel != null) caloriesLabel.setText("1,845");
        if (sleepLabel != null) sleepLabel.setText("7h 23m");
        if (waterLabel != null) waterLabel.setText("1.8L");
        if (weightLabel != null) weightLabel.setText("68.5 kg");
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        try {
            // Stop the clock
            if (clockTimeline != null) {
                clockTimeline.stop();
            }

            // Load login screen
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/Login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) logoutBtn.getScene().getWindow();
            Scene scene = new Scene(root);

            // Fade transition
            FadeTransition ft = new FadeTransition(Duration.millis(300), root);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.play();

            stage.setScene(scene);
            stage.centerOnScreen();
            stage.setMaximized(false);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Updated methods to work with User object
    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        updateUserDisplay();
        loadDashboardData();
    }

    // For backward compatibility with String username
    public void setCurrentUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setFullName(username); // Fallback
        setCurrentUser(user);
    }

    private void updateUserDisplay() {
        if (currentUser != null && userNameLabel != null) {
            // Show full name if available, otherwise username
            String displayName = currentUser.getFullName() != null && !currentUser.getFullName().trim().isEmpty()
                    ? currentUser.getFullName()
                    : currentUser.getUsername();
            userNameLabel.setText(displayName);
        }
    }
}