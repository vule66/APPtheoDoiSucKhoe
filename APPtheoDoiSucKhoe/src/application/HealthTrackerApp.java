package application;

import database.DatabaseManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

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
        // Thử kết nối đến máy chủ trước khi khởi động một máy chủ mới
        boolean serverRunning = isServerRunning("localhost", 9876);

        if (!serverRunning) {
            Thread serverThread = new Thread(() -> {
                try {
                    System.out.println("Khởi động máy chủ chat mới...");
                    server.ChatServer.main(new String[]{});
                } catch (Exception e) {
                    System.out.println("Lỗi khởi động máy chủ chat: " + e.getMessage());
                    e.printStackTrace();
                }
            });
            serverThread.setDaemon(true);
            serverThread.start();

            try {
                Thread.sleep(1000); // Đợi máy chủ khởi động
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Đã phát hiện máy chủ chat đang chạy, sẽ kết nối đến máy chủ hiện có");
        }

        launch(args);
    }

    // Phương thức kiểm tra xem máy chủ đã chạy chưa
    private static boolean isServerRunning(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 1000);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}