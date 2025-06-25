package controllers;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.scene.Node;
import database.ChatClient;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
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
    }

    public void initializeChat(String username) {
        this.username = username;
        System.out.println("Khởi tạo chat cho người dùng: " + username);

        chatClient = new ChatClient(username, chatArea, userListView);

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
        } else {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Lỗi");
            alert.setHeaderText("Không thể gửi tin nhắn");
            alert.setContentText("Đã mất kết nối đến máy chủ chat. Vui lòng thử lại sau.");
            alert.showAndWait();
        }
    }

    public Node getRoot() {
        if (rootPane != null) {
            if (!rootPane.getStyleClass().contains("chat-container")) {
                rootPane.getStyleClass().add("chat-container");
            }
            return rootPane;
        }

        if (chatArea != null) {
            Parent parent = chatArea.getParent();
            while (parent != null) {
                if (parent instanceof AnchorPane) {
                    if (!parent.getStyleClass().contains("chat-container")) {
                        parent.getStyleClass().add("chat-container");
                    }
                    return parent;
                }
                parent = parent.getParent();
            }
        }

        if (userListView != null) {
            Parent parent = userListView.getParent();
            while (parent != null) {
                if (parent instanceof AnchorPane) {

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

    public void disconnectChat() {
        if (chatClient != null) {
            chatClient.disconnect();
            System.out.println("Đã ngắt kết nối chat");
        }
    }
}