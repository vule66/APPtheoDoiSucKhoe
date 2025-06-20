package controllers;

import database.DatabaseManager;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.User;
import services.UserService;

import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox rememberMeCheck;
    @FXML private Button loginButton;
    @FXML private Hyperlink forgotPasswordLink;
    @FXML private Hyperlink signUpLink;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator loadingIndicator;

    private UserService userService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("✅ LoginController initialized!");

        try {
            // Initialize database first
            DatabaseManager.getInstance();
            userService = new UserService();
            System.out.println("✅ UserService created successfully!");
        } catch (Exception e) {
            System.out.println("❌ Error creating UserService: " + e.getMessage());
            e.printStackTrace();
            showStatus("Database connection failed. Please restart the application.", false);
        }

        // Auto-fill for testing
        if (usernameField != null) {
            usernameField.setText("vule66");
            System.out.println("✅ Username field auto-filled");
        }
        if (passwordField != null) {
            passwordField.setText("123456");
            System.out.println("✅ Password field auto-filled");
        }

        // Hide loading indicator initially
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(false);
        }

        // Setup event handlers
        setupEventHandlers();
    }

    private void setupEventHandlers() {
        // Enter key support
        if (passwordField != null) {
            passwordField.setOnAction(this::handleLogin);
        }
        if (usernameField != null) {
            usernameField.setOnAction(e -> passwordField.requestFocus());
        }
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        System.out.println("🚀 LOGIN BUTTON CLICKED!");

        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        System.out.println("Username: '" + username + "'");
        System.out.println("Password: '" + password + "'");

        // Validation
        if (username.isEmpty() || password.isEmpty()) {
            showStatus("Please enter both username and password.", false);
            return;
        }

        // Disable login button and show loading
        loginButton.setDisable(true);
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(true);
        }
        showStatus("Authenticating...", true);

        // Perform authentication in background thread
        Task<User> authTask = new Task<User>() {
            @Override
            protected User call() throws Exception {
                Thread.sleep(500); // Simulate network delay
                return userService.authenticateUser(username, password);
            }

            @Override
            protected void succeeded() {
                User user = getValue();
                loginButton.setDisable(false);
                if (loadingIndicator != null) {
                    loadingIndicator.setVisible(false);
                }

                if (user != null) {
                    System.out.println("✅ Authentication successful! User: " + user.toString());
                    showStatus("Login successful! Welcome " + user.getFullName(), true);
                    openMainApplication(user);
                } else {
                    System.out.println("❌ Authentication failed - invalid credentials");
                    showStatus("Invalid username or password. Please try again.", false);
                }
            }

            @Override
            protected void failed() {
                loginButton.setDisable(false);
                if (loadingIndicator != null) {
                    loadingIndicator.setVisible(false);
                }
                System.out.println("❌ Authentication task failed: " + getException().getMessage());
                showStatus("Login failed. Please try again.", false);
            }
        };

        Thread authThread = new Thread(authTask);
        authThread.setDaemon(true);
        authThread.start();
    }

    private void openMainApplication(User user) {
        try {
            System.out.println("Loading main application...");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/HealthTrackingApp.fxml"));
            Parent root = loader.load();

            // Pass user data to main controller
            HealthTrackerController controller = loader.getController();
            if (controller != null) {
                controller.setCurrentUser(user);
                System.out.println("✅ User data passed to HealthTrackerController");
            }

            Stage stage = (Stage) loginButton.getScene().getWindow();
            Scene scene = new Scene(root);

            stage.setScene(scene);
            stage.setTitle("Health Tracker Pro - Dashboard");
            stage.setMaximized(true);

            System.out.println("✅ Main application loaded successfully!");

        } catch (Exception e) {
            System.out.println("❌ Error loading main application: " + e.getMessage());
            e.printStackTrace();
            showStatus("Failed to load main application: " + e.getMessage(), false);
        }
    }

    @FXML
    private void handleSignUp(ActionEvent event) {
        System.out.println("🔗 Sign Up clicked!");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/Register.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) signUpLink.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Health Tracker Pro - Sign Up");

        } catch (Exception e) {
            System.out.println("❌ Error loading registration page: " + e.getMessage());
            e.printStackTrace();
            showStatus("Failed to load registration page: " + e.getMessage(), false);
        }
    }

    @FXML
    private void handleForgotPassword(ActionEvent event) {
        System.out.println("🔗 Forgot Password clicked!");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/ForgotPassword.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) forgotPasswordLink.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Health Tracker Pro - Reset Password");

        } catch (Exception e) {
            System.out.println("❌ Error loading forgot password page: " + e.getMessage());
            e.printStackTrace();
            showStatus("Failed to load forgot password page: " + e.getMessage(), false);
        }
    }

    private void showStatus(String message, boolean isSuccess) {
        if (statusLabel != null) {
            statusLabel.setText(message);
            statusLabel.setStyle(isSuccess ?
                    "-fx-text-fill: #10b981; -fx-font-weight: bold;" :
                    "-fx-text-fill: #ef4444; -fx-font-weight: bold;");
        }
        System.out.println("Status: " + message);
    }
}