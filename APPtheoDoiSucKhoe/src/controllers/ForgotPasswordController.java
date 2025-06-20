package controllers;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import services.UserService;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

public class ForgotPasswordController implements Initializable {

    @FXML private TextField emailField;
    @FXML private Button resetButton;
    @FXML private Hyperlink backToLoginLink;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator loadingIndicator;

    private UserService userService;
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("✅ ForgotPasswordController initialized!");

        try {
            userService = new UserService();
            System.out.println("✅ UserService created for password reset");
        } catch (Exception e) {
            System.out.println("❌ Error creating UserService: " + e.getMessage());
            e.printStackTrace();
        }

        // Hide loading indicator
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(false);
        }

        // Auto-fill for testing
        if (emailField != null) {
            emailField.setText("vule66@example.com");
            System.out.println("✅ Email field auto-filled for testing");
        }

        setupEventHandlers();
    }

    private void setupEventHandlers() {
        // Enter key support
        if (emailField != null) {
            emailField.setOnAction(this::handleResetPassword);
        }

        // Real-time email validation
        if (emailField != null) {
            emailField.textProperty().addListener((obs, oldText, newText) -> {
                if (!newText.isEmpty()) {
                    if (isValidEmail(newText)) {
                        emailField.setStyle("-fx-border-color: #10b981; -fx-border-width: 2;");
                    } else {
                        emailField.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2;");
                    }
                } else {
                    emailField.setStyle("");
                }
            });
        }
    }

    @FXML
    private void handleResetPassword(ActionEvent event) {
        System.out.println("🚀 RESET PASSWORD BUTTON CLICKED!");

        String email = emailField.getText().trim();
        System.out.println("Email: '" + email + "'");

        // Validation
        if (email.isEmpty()) {
            showStatus("Please enter your email address.", false);
            return;
        }

        if (!isValidEmail(email)) {
            showStatus("Please enter a valid email address.", false);
            return;
        }

        // Disable reset button and show loading
        resetButton.setDisable(true);
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(true);
        }
        showStatus("Checking email...", true);

        // Perform email check and password reset in background thread
        Task<Boolean> resetTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                Thread.sleep(1500); // Simulate processing

                // Check if email exists
                boolean emailExists = userService.isEmailExists(email);
                if (emailExists) {
                    // In real application, you would send email here
                    // For demo, we'll just simulate success
                    System.out.println("Email exists, would send reset link to: " + email);
                    return true;
                } else {
                    System.out.println("Email not found: " + email);
                    return false;
                }
            }

            @Override
            protected void succeeded() {
                resetButton.setDisable(false);
                if (loadingIndicator != null) {
                    loadingIndicator.setVisible(false);
                }

                if (getValue()) {
                    System.out.println("✅ Reset request processed successfully");
                    showStatus("Password reset link sent to your email! Check your inbox.", true);

                    // Show additional instructions
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Reset Link Sent");
                    alert.setHeaderText("Check Your Email");
                    alert.setContentText("We've sent a password reset link to " + email +
                            "\n\nIf you don't receive it within a few minutes, please:" +
                            "\n• Check your spam folder" +
                            "\n• Verify the email address is correct" +
                            "\n• Try again later" +
                            "\n\nThe link will expire in 1 hour for security.");
                    alert.showAndWait();

                    // Auto-redirect to login after 3 seconds
                    Task<Void> redirectTask = new Task<Void>() {
                        @Override
                        protected Void call() throws Exception {
                            Thread.sleep(3000);
                            return null;
                        }

                        @Override
                        protected void succeeded() {
                            handleBackToLogin(null);
                        }
                    };

                    Thread redirectThread = new Thread(redirectTask);
                    redirectThread.setDaemon(true);
                    redirectThread.start();

                } else {
                    System.out.println("❌ Email not found");
                    showStatus("Email address not found. Please check and try again.", false);
                }
            }

            @Override
            protected void failed() {
                resetButton.setDisable(false);
                if (loadingIndicator != null) {
                    loadingIndicator.setVisible(false);
                }
                System.out.println("❌ Reset task failed: " + getException().getMessage());
                showStatus("Password reset failed. Please try again.", false);
            }
        };

        Thread resetThread = new Thread(resetTask);
        resetThread.setDaemon(true);
        resetThread.start();
    }

    @FXML
    private void handleBackToLogin(ActionEvent event) {
        System.out.println("🔗 Back to Login clicked!");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/Login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) (backToLoginLink != null ?
                    backToLoginLink.getScene().getWindow() :
                    resetButton.getScene().getWindow());
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Health Tracker Pro - Login");

        } catch (Exception e) {
            System.out.println("❌ Error loading login page: " + e.getMessage());
            e.printStackTrace();
            showStatus("Failed to load login page: " + e.getMessage(), false);
        }
    }

    private boolean isValidEmail(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
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