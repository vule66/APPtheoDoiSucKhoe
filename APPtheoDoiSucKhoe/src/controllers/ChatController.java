package controllers;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.scene.Node;
import database.ChatClient;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.AnchorPane;


public class ChatController implements Initializable {

    @FXML
    private TextArea chatArea;

    @FXML
    private TextField messageField;

    @FXML
    private ListView<String> userListView;

    @FXML
    private AnchorPane rootPane;

    private ChatClient chatClient;
    private String username;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Chỉ khởi tạo giao diện, kết nối sẽ được thực hiện trong initializeChat
    }

    // Phương thức setUserData cũ (giữ lại để tương thích với mã cũ nếu cần)
    public void setUserData(String username, javafx.stage.Stage stage) {
        this.username = username;

        // Tạo chat client với người dùng hiện tại
        chatClient = new ChatClient(username, chatArea, userListView);

        // Thử kết nối đến máy chủ chat
        if (!chatClient.connect()) {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Lỗi kết nối");
            alert.setHeaderText("Không thể kết nối đến máy chủ chat");
            alert.setContentText("Vui lòng thử lại sau hoặc liên hệ hỗ trợ.");
            alert.showAndWait();
        } else {
            chatArea.appendText("Đã kết nối đến chat với tên " + username + "\n");
            chatArea.appendText("Chào mừng đến với chat cộng đồng! Hãy tôn trọng và hỗ trợ nhau.\n");
        }

        // Xử lý sự kiện đóng cửa sổ
        if (stage != null) {
            stage.setOnCloseRequest(event -> {
                if (chatClient.isConnected()) {
                    chatClient.disconnect();
                }
            });
        }
    }
    public void initializeChat(String username) {
        this.username = username;
        System.out.println("Khởi tạo chat cho người dùng: " + username);

        // Tạo chat client với người dùng hiện tại
        chatClient = new ChatClient(username, chatArea, userListView);

        // Thử kết nối đến máy chủ chat
        if (!chatClient.connect()) {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Lỗi kết nối");
            alert.setHeaderText("Không thể kết nối đến máy chủ chat");
            alert.setContentText("Vui lòng đảm bảo máy chủ chat đang chạy và thử lại sau.");
            alert.showAndWait();
        } else {
            chatArea.appendText("Đã kết nối đến chat với tên " + username + "\n");
            chatArea.appendText("Chào mừng đến với chat cộng đồng! Hãy tôn trọng và hỗ trợ nhau.\n");
        }
    }

    @FXML
    void sendMessage(ActionEvent event) {
        String message = messageField.getText().trim();
        if (!message.isEmpty() && chatClient != null && chatClient.isConnected()) {
            chatClient.sendMessage(message);
            messageField.clear();
        } else if (message.isEmpty()) {
            // Không làm gì nếu tin nhắn trống
        } else {
            // Hiển thị lỗi nếu không thể gửi
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Lỗi");
            alert.setHeaderText("Không thể gửi tin nhắn");
            alert.setContentText("Đã mất kết nối đến máy chủ chat. Vui lòng thử lại sau.");
            alert.showAndWait();
        }
    }

    public Node getRoot() {
        // Ưu tiên sử dụng rootPane nếu đã được khai báo trong FXML
        if (rootPane != null) {
            // Đảm bảo rootPane có style class phù hợp
            if (!rootPane.getStyleClass().contains("chat-container")) {
                rootPane.getStyleClass().add("chat-container");
            }
            return rootPane;
        }

        // Fallback: Tìm AnchorPane từ chatArea
        if (chatArea != null) {
            Parent parent = chatArea.getParent();
            while (parent != null) {
                if (parent instanceof AnchorPane) {
                    // Thêm style class cho AnchorPane tìm được
                    if (!parent.getStyleClass().contains("chat-container")) {
                        parent.getStyleClass().add("chat-container");
                    }
                    return parent;
                }
                parent = parent.getParent();
            }
        }

        // Fallback: Tìm AnchorPane từ userListView
        if (userListView != null) {
            Parent parent = userListView.getParent();
            while (parent != null) {
                if (parent instanceof AnchorPane) {
                    // Thêm style class cho AnchorPane tìm được
                    if (!parent.getStyleClass().contains("chat-container")) {
                        parent.getStyleClass().add("chat-container");
                    }
                    return parent;
                }
                parent = parent.getParent();
            }
        }

        System.err.println("Không thể tìm thấy node gốc trong ChatController");
        return null;
    }

    // Phương thức để ngắt kết nối khi chuyển đến màn hình khác
    public void disconnectChat() {
        if (chatClient != null) {
            chatClient.disconnect();
            System.out.println("Đã ngắt kết nối chat");
        }
    }
}