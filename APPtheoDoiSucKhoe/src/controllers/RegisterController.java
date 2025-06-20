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
import models.User;
import services.UserService;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

public class RegisterController implements Initializable {

    @FXML private TextField fullNameField;
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField ageField;
    @FXML private ComboBox<String> genderCombo;
    @FXML private TextField heightField;
    @FXML private Button registerButton;
    @FXML private Hyperlink backToLoginLink;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator loadingIndicator;

    private UserService userService;
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("✅ RegisterController initialized!");

        try {
            userService = new UserService();
            System.out.println("✅ UserService created for registration");
        } catch (Exception e) {
            System.out.println("❌ Error creating UserService: " + e.getMessage());
            e.printStackTrace();
        }

        // Setup gender combo
        if (genderCombo != null) {
            genderCombo.getItems().addAll("Male", "Female", "Other");
            System.out.println("✅ Gender combo populated");
        }

        // Hide loading indicator
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(false);
        }

        setupEventHandlers();
        setupSampleData(); // For testing
    }

    private void setupSampleData() {
        // Auto-fill for testing
        if (fullNameField != null) fullNameField.setText("Test User");
        if (usernameField != null) usernameField.setText("testuser123");
        if (emailField != null) emailField.setText("test@example.com");
        if (passwordField != null) passwordField.setText("123456");
        if (confirmPasswordField != null) confirmPasswordField.setText("123456");
        if (ageField != null) ageField.setText("25");
        if (genderCombo != null) genderCombo.setValue("Male");
        if (heightField != null) heightField.setText("175");
        System.out.println("✅ Sample data filled for testing");
    }

    private void setupEventHandlers() {
        // Real-time validation
        if (usernameField != null) {
            usernameField.textProperty().addListener((obs, oldText, newText) -> {
                if (newText.length() > 3) {
                    checkUsernameAvailability(newText);
                }
            });
        }

        if (emailField != null) {
            emailField.textProperty().addListener((obs, oldText, newText) -> {
                if (isValidEmail(newText)) {
                    checkEmailAvailability(newText);
                }
            });
        }

        // Password matching
        if (confirmPasswordField != null) {
            confirmPasswordField.textProperty().addListener((obs, oldText, newText) -> {
                checkPasswordMatch();
            });
        }

        if (passwordField != null) {
            passwordField.textProperty().addListener((obs, oldText, newText) -> {
                checkPasswordMatch();
            });
        }
    }

    private void checkPasswordMatch() {
        if (passwordField != null && confirmPasswordField != null) {
            String password = passwordField.getText();
            String confirmPassword = confirmPasswordField.getText();

            if (!confirmPassword.isEmpty()) {
                if (password.equals(confirmPassword)) {
                    confirmPasswordField.setStyle("-fx-border-color: #10b981; -fx-border-width: 2;");
                } else {
                    confirmPasswordField.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2;");
                }
            } else {
                confirmPasswordField.setStyle("");
            }
        }
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        System.out.println("🚀 REGISTER BUTTON CLICKED!");

        if (!validateForm()) {
            return;
        }

        // Disable register button and show loading
        registerButton.setDisable(true);
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(true);
        }
        showStatus("Creating account...", true);

        // Create user object
        User user = new User();
        user.setFullName(fullNameField.getText().trim());
        user.setUsername(usernameField.getText().trim());
        user.setEmail(emailField.getText().trim());
        user.setAge(Integer.parseInt(ageField.getText()));
        user.setGender(genderCombo.getValue());
        user.setHeight(Double.parseDouble(heightField.getText()));

        String password = passwordField.getText();

        System.out.println("Creating user: " + user.toString());

        // Perform registration in background thread
        Task<Boolean> registerTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                Thread.sleep(1000); // Simulate processing
                return userService.registerUser(user, password);
            }

            @Override
            protected void succeeded() {
                registerButton.setDisable(false);
                if (loadingIndicator != null) {
                    loadingIndicator.setVisible(false);
                }

                if (getValue()) {
                    System.out.println("✅ Registration successful!");
                    showStatus("Account created successfully! Redirecting to login...", true);

                    // Redirect to login after 2 seconds
                    Task<Void> redirectTask = new Task<Void>() {
                        @Override
                        protected Void call() throws Exception {
                            Thread.sleep(2000);
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
                    System.out.println("❌ Registration failed");
                    showStatus("Registration failed. Username or email may already exist.", false);
                }
            }

            @Override
            protected void failed() {
                registerButton.setDisable(false);
                if (loadingIndicator != null) {
                    loadingIndicator.setVisible(false);
                }
                System.out.println("❌ Registration task failed: " + getException().getMessage());
                showStatus("Registration failed. Please try again.", false);
            }
        };

        Thread registerThread = new Thread(registerTask);
        registerThread.setDaemon(true);
        registerThread.start();
    }

    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();

        // Full name validation
        String fullName = fullNameField.getText().trim();
        if (fullName.length() < 2) {
            errors.append("• Please enter your full name\n");
        }

        // Username validation
        String username = usernameField.getText().trim();
        if (username.length() < 4) {
            errors.append("• Username must be at least 4 characters long\n");
        }

        // Email validation
        String email = emailField.getText().trim();
        if (!isValidEmail(email)) {
            errors.append("• Please enter a valid email address\n");
        }

        // Password validation
        String password = passwordField.getText();
        if (password.length() < 6) {
            errors.append("• Password must be at least 6 characters long\n");
        }

        // Confirm password validation
        String confirmPassword = confirmPasswordField.getText();
        if (!password.equals(confirmPassword)) {
            errors.append("• Passwords do not match\n");
        }

        // Age validation
        try {
            int age = Integer.parseInt(ageField.getText());
            if (age < 13 || age > 120) {
                errors.append("• Please enter a valid age (13-120)\n");
            }
        } catch (NumberFormatException e) {
            errors.append("• Please enter a valid age\n");
        }

        // Gender validation
        if (genderCombo.getValue() == null) {
            errors.append("• Please select your gender\n");
        }

        // Height validation
        try {
            double height = Double.parseDouble(heightField.getText());
            if (height < 100 || height > 250) {
                errors.append("• Please enter a valid height (100-250 cm)\n");
            }
        } catch (NumberFormatException e) {
            errors.append("• Please enter a valid height\n");
        }

        if (errors.length() > 0) {
            showStatus(errors.toString().trim(), false);
            return false;
        }

        return true;
    }

    private boolean isValidEmail(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }

    private void checkUsernameAvailability(String username) {
        if (userService == null) return;

        Task<Boolean> checkTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                return userService.isUsernameExists(username);
            }

            @Override
            protected void succeeded() {
                if (getValue()) {
                    usernameField.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2;");
                } else {
                    usernameField.setStyle("-fx-border-color: #10b981; -fx-border-width: 2;");
                }
            }
        };

        Thread checkThread = new Thread(checkTask);
        checkThread.setDaemon(true);
        checkThread.start();
    }

    private void checkEmailAvailability(String email) {
        if (userService == null) return;

        Task<Boolean> checkTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                return userService.isEmailExists(email);
            }

            @Override
            protected void succeeded() {
                if (getValue()) {
                    emailField.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2;");
                } else {
                    emailField.setStyle("-fx-border-color: #10b981; -fx-border-width: 2;");
                }
            }
        };

        Thread checkThread = new Thread(checkTask);
        checkThread.setDaemon(true);
        checkThread.start();
    }

    @FXML
    private void handleBackToLogin(ActionEvent event) {
        System.out.println("🔗 Back to Login clicked!");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/Login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) (backToLoginLink != null ?
                    backToLoginLink.getScene().getWindow() :
                    registerButton.getScene().getWindow());
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Health Tracker Pro - Login");

        } catch (Exception e) {
            System.out.println("❌ Error loading login page: " + e.getMessage());
            e.printStackTrace();
            showStatus("Failed to load login page: " + e.getMessage(), false);
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