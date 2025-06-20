package application;

import database.DatabaseManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class HealthTrackerApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            System.out.println("🚀 Starting Health Tracker Application...");

            // Initialize database first
            DatabaseManager.getInstance();
            System.out.println("✅ Database initialized successfully!");

            // Load login screen
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Login.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root);

            // Set application icon (optional)
            try {
                primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/app-icon.png")));
            } catch (Exception e) {
                System.out.println("ℹ️ App icon not found, using default");
            }

            primaryStage.setTitle("Health Tracker Pro - Login");
            primaryStage.setScene(scene);
            primaryStage.setResizable(false);
            primaryStage.centerOnScreen();
            primaryStage.show();

            System.out.println("✅ Health Tracker Application started successfully!");
            System.out.println("🔑 Default login: vule66 / 123456");

        } catch(Exception e) {
            System.out.println("❌ Error starting application: " + e.getMessage());
            e.printStackTrace();

            // Show error dialog
            Platform.runLater(() -> {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                alert.setTitle("Application Error");
                alert.setHeaderText("Failed to start Health Tracker");
                alert.setContentText("Error: " + e.getMessage());
                alert.showAndWait();
                Platform.exit();
            });
        }
    }

    @Override
    public void stop() {
        // Close database connection when app closes
        try {
            DatabaseManager.getInstance().closeConnection();
            System.out.println("✅ Application closed and database disconnected.");
        } catch (Exception e) {
            System.out.println("❌ Error during application shutdown: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        System.out.println("====================================");
        System.out.println("🏥 HEALTH TRACKER PRO v1.0");
        System.out.println("====================================");
        launch(args);
    }
}