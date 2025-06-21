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

    // --- FXML Elements ---
    @FXML private StackPane mainContentPane;
    @FXML private Label userNameLabel;
    @FXML private Label userInitialsLabel;
    @FXML private Label dateTimeLabel;

    // --- Navigation Buttons ---
    @FXML private Button dashboardBtn;
    @FXML private Button dataEntryBtn;
    @FXML private Button analyticsBtn;
    @FXML private Button goalsBtn;
    @FXML private Button historyBtn;
    @FXML private Button settingsBtn;
    @FXML private Button logoutBtn;

    // --- State Variables ---
    private Button currentActiveButton;
    private User currentUser;
    private Timeline clockTimeline;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // PHẦN KHỞI TẠO: Chỉ làm những việc không phụ thuộc vào người dùng
        startClock();
    }

    /**
     * Phương thức này là "cổng chính" nhận dữ liệu từ LoginController.
     * Mọi hành động phụ thuộc vào User sẽ bắt đầu từ đây.
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (this.currentUser != null) {
            updateUserDisplay();
            // SỬA LỖI: Tải Dashboard làm màn hình mặc định và KÍCH HOẠT nút.
            // Chúng ta giả lập một cú click chuột lên nút Dashboard.
            handleMenuClick(new ActionEvent(dashboardBtn, dashboardBtn));
        } else {
            mainContentPane.getChildren().setAll(new Label("Critical Error: Could not load user data."));
        }
    }

    /**
     * PHƯƠNG THỨC XỬ LÝ MENU ĐÃ SỬA LỖI HOÀN CHỈNH.
     * Đây là nơi bạn muốn sửa code.
     */
    @FXML
    private void handleMenuClick(ActionEvent event) {
        Button clickedButton = (Button) event.getSource();

        // Nếu nhấn lại nút đang sáng -> không làm gì cả
        if (clickedButton == currentActiveButton && mainContentPane.getChildren().size() > 0) {
            return;
        }

        // SỬA LỖI LỚN: Bỏ cách suy luận tên file.
        // Dùng if-else if để chỉ định chính xác tên file FXML.
        String fxmlFileToLoad = "";
        if (clickedButton == dashboardBtn) {
            fxmlFileToLoad = "/application/DashboardView.fxml";
        } else if (clickedButton == dataEntryBtn) {
            // ĐÂY LÀ CHỖ SỬA QUAN TRỌNG NHẤT: Đảm bảo đúng tên file
            fxmlFileToLoad = "/application/DataEntryView.fxml";
        } else if (clickedButton == analyticsBtn) {
            // Ví dụ cho các nút chưa làm
            fxmlFileToLoad = "/application/AnalyticsView.fxml";
        } else if (clickedButton == goalsBtn) {
            fxmlFileToLoad = "/application/GoalsView.fxml";
        } else if (clickedButton == historyBtn) {
            fxmlFileToLoad = "/application/HistoryView.fxml";
        } else if (clickedButton == settingsBtn) {
            fxmlFileToLoad = "/application/SettingsView.fxml";
        }

        // Chỉ tải view và cập nhật nút khi có đường dẫn hợp lệ
        if (!fxmlFileToLoad.isEmpty()) {
            loadView(fxmlFileToLoad);
            updateActiveButton(clickedButton); // Cập nhật nút sáng
        } else {
            // Xử lý cho các nút chưa có giao diện
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
            // Thêm kiểm tra null để chắc chắn file tồn tại
            URL resourceUrl = getClass().getResource(fxmlPath);
            if (resourceUrl == null) {
                System.err.println("FATAL: FXML file not found at path: " + fxmlPath);
                mainContentPane.getChildren().setAll(new Label("Error: View file not found."));
                return;
            }

            FXMLLoader loader = new FXMLLoader(resourceUrl);
            Node view = loader.load();

            // Truyền currentUser cho các controller cần nó
            Object controller = loader.getController();
            if (controller instanceof DashboardController) {
                ((DashboardController) controller).setCurrentUser(this.currentUser);
            } else if (controller instanceof DataEntryController) {
                ((DataEntryController) controller).setCurrentUser(this.currentUser);
            }
            else if (controller instanceof AnalyticsController) {
                ((AnalyticsController) controller).setCurrentUser(this.currentUser);
            }
            // Thêm các trường hợp else if cho các controller khác trong tương lai

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

    // ----- CÁC PHƯƠNG THỨC KHÁC GIỮ NGUYÊN -----

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