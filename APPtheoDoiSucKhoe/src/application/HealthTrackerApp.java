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
            System.out.println("Starting Health Tracker Application...");

            DatabaseManager.getInstance();
            System.out.println("Database initialized successfully!");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("Login.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root);

            try {
                primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/app-icon.png")));
            } catch (Exception e) {
                System.out.println("App icon not found, using default");
            }

            primaryStage.setTitle("Health Tracker Pro - Login");
            primaryStage.setScene(scene);
            primaryStage.setResizable(false);
            primaryStage.centerOnScreen();
            primaryStage.show();

        } catch(Exception e) {
            System.out.println("Error starting application: " + e.getMessage());
            e.printStackTrace();

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
        try {
            DatabaseManager.getInstance().closeConnection();
            System.out.println("Application closed and database disconnected.");
        } catch (Exception e) {
            System.out.println("Error during application shutdown: " + e.getMessage());
        }
    }

    public static void main(String[] args) {

        launch(args);
    }
}