package controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.util.Duration;
import models.ChatMessage;
import models.User;
import services.ChatService;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class ChatController implements Initializable {

    @FXML private VBox chatContainer;
    @FXML private ScrollPane chatScrollPane;
    @FXML private TextArea messageInput;
    @FXML private Button sendButton;
    @FXML private ListView<User> onlineUsersList;
    @FXML private Label onlineCountLabel;
    @FXML private Label currentUserLabel;

    private User currentUser;
    private ChatService chatService;
    private Timeline refreshTimeline;
    private LocalDateTime lastMessageTime;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        chatService = new ChatService();
        setupUI();
        setupAutoRefresh();
    }

    public void setCurrentUser(User user) {
        if (this.currentUser != null) {
            // Mark previous user as offline
            chatService.updateUserOnlineStatus(this.currentUser.getId(), false);
        }

        this.currentUser = user;
        if (this.currentUser != null) {
            currentUserLabel.setText("Chat as: " + getCurrentUserDisplayName());
            // Mark current user as online
            chatService.updateUserOnlineStatus(this.currentUser.getId(), true);
            loadInitialMessages();
            refreshOnlineUsers();
        }
    }

    private void setupUI() {
        // Setup message input
        messageInput.setPromptText("Type your message here... (Press Enter to send, Shift+Enter for new line)");
        messageInput.setWrapText(true);
        messageInput.setPrefRowCount(2);

        // Setup scroll pane
        chatScrollPane.setFitToWidth(true);
        chatScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        chatScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        // Setup send button
        sendButton.setOnAction(e -> sendMessage());

        // Setup keyboard shortcuts
        messageInput.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (event.isShiftDown()) {
                    // Shift+Enter: new line (default behavior)
                    return;
                } else {
                    // Enter: send message
                    event.consume();
                    sendMessage();
                }
            }
        });

        // Setup online users list
        onlineUsersList.setCellFactory(listView -> new ListCell<User>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox container = new HBox(10);
                    container.setAlignment(Pos.CENTER_LEFT);

                    // Online indicator
                    Circle onlineIndicator = new Circle(4);
                    onlineIndicator.setFill(javafx.scene.paint.Color.GREEN);

                    // User name
                    Label nameLabel = new Label(user.getFullName() != null && !user.getFullName().trim().isEmpty()
                            ? user.getFullName() : user.getUsername());

                    container.getChildren().addAll(onlineIndicator, nameLabel);
                    setGraphic(container);
                }
            }
        });
    }

    private void setupAutoRefresh() {
        // Refresh messages every 2 seconds
        refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
            if (currentUser != null) {
                refreshMessages();
                refreshOnlineUsers();
            }
        }));
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
    }

    private void loadInitialMessages() {
        Platform.runLater(() -> {
            chatContainer.getChildren().clear();
            List<ChatMessage> messages = chatService.getRecentMessages(50);

            for (ChatMessage message : messages) {
                addMessageToUI(message);
            }

            if (!messages.isEmpty()) {
                lastMessageTime = messages.get(messages.size() - 1).getTimestamp();
            } else {
                lastMessageTime = LocalDateTime.now();
            }

            scrollToBottom();
        });
    }

    private void refreshMessages() {
        if (lastMessageTime == null) {
            lastMessageTime = LocalDateTime.now().minusMinutes(1);
        }

        List<ChatMessage> newMessages = chatService.getMessagesAfter(lastMessageTime);

        if (!newMessages.isEmpty()) {
            Platform.runLater(() -> {
                for (ChatMessage message : newMessages) {
                    addMessageToUI(message);
                }
                lastMessageTime = newMessages.get(newMessages.size() - 1).getTimestamp();
                scrollToBottom();
            });
        }
    }

    private void refreshOnlineUsers() {
        List<User> onlineUsers = chatService.getOnlineUsers();

        Platform.runLater(() -> {
            onlineUsersList.getItems().clear();
            onlineUsersList.getItems().addAll(onlineUsers);
            onlineCountLabel.setText("Online: " + onlineUsers.size());
        });
    }

    private void sendMessage() {
        String content = messageInput.getText().trim();
        if (content.isEmpty() || currentUser == null) {
            return;
        }

        ChatMessage message = new ChatMessage(
                currentUser.getId(),
                currentUser.getUsername(),
                content
        );
        message.setSenderFullName(currentUser.getFullName());

        if (chatService.sendMessage(message)) {
            messageInput.clear();
            // Message will be displayed when refresh happens
        } else {
            showAlert("Error", "Failed to send message. Please try again.");
        }
    }

    private void addMessageToUI(ChatMessage message) {
        VBox messageBox = createMessageBox(message);
        chatContainer.getChildren().add(messageBox);
    }

    private VBox createMessageBox(ChatMessage message) {
        VBox messageBox = new VBox(5);
        messageBox.setPadding(new Insets(10));
        messageBox.setMaxWidth(Double.MAX_VALUE);

        // Check if this is current user's message
        boolean isCurrentUser = currentUser != null && message.getSenderId() == currentUser.getId();

        if (isCurrentUser) {
            messageBox.setAlignment(Pos.CENTER_RIGHT);
            messageBox.setStyle("-fx-background-color: #007bff; -fx-background-radius: 15px; -fx-text-fill: white;");
        } else {
            messageBox.setAlignment(Pos.CENTER_LEFT);
            messageBox.setStyle("-fx-background-color: #f1f3f4; -fx-background-radius: 15px;");
        }

        // Create header (sender name and time)
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(isCurrentUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        Label senderLabel = new Label(getSenderDisplayName(message));
        senderLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

        Label timeLabel = new Label(message.getTimestamp().format(TIME_FORMATTER));
        timeLabel.setStyle("-fx-font-size: 10px; -fx-opacity: 0.7;");

        if (isCurrentUser) {
            headerBox.getChildren().addAll(timeLabel, senderLabel);
        } else {
            headerBox.getChildren().addAll(senderLabel, timeLabel);
        }

        // Create content
        Label contentLabel = new Label(message.getContent());
        contentLabel.setWrapText(true);
        contentLabel.setMaxWidth(400);
        contentLabel.setStyle("-fx-font-size: 14px;");
        if (isCurrentUser) {
            contentLabel.setStyle(contentLabel.getStyle() + " -fx-text-fill: white;");
        }

        // Add edited indicator if needed
        if (message.isEdited()) {
            Label editedLabel = new Label("(edited)");
            editedLabel.setStyle("-fx-font-size: 10px; -fx-opacity: 0.5; -fx-font-style: italic;");
            if (isCurrentUser) {
                editedLabel.setStyle(editedLabel.getStyle() + " -fx-text-fill: white;");
            }

            VBox contentBox = new VBox(2);
            contentBox.getChildren().addAll(contentLabel, editedLabel);
            messageBox.getChildren().addAll(headerBox, contentBox);
        } else {
            messageBox.getChildren().addAll(headerBox, contentLabel);
        }

        // Add right-click menu for current user's messages
        if (isCurrentUser) {
            setupMessageContextMenu(messageBox, message);
        }

        return messageBox;
    }

    private void setupMessageContextMenu(VBox messageBox, ChatMessage message) {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem editItem = new MenuItem("Edit");
        editItem.setOnAction(e -> editMessage(message));

        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(e -> deleteMessage(message));

        contextMenu.getItems().addAll(editItem, deleteItem);
        messageBox.setOnContextMenuRequested(e -> contextMenu.show(messageBox, e.getScreenX(), e.getScreenY()));
    }

    private void editMessage(ChatMessage message) {
        TextInputDialog dialog = new TextInputDialog(message.getContent());
        dialog.setTitle("Edit Message");
        dialog.setHeaderText("Edit your message:");
        dialog.setContentText("Message:");

        dialog.showAndWait().ifPresent(newContent -> {
            if (!newContent.trim().isEmpty() && !newContent.equals(message.getContent())) {
                if (chatService.editMessage(message.getId(), newContent.trim(), currentUser.getId())) {
                    // Refresh messages to show the edit
                    loadInitialMessages();
                } else {
                    showAlert("Error", "Failed to edit message.");
                }
            }
        });
    }

    private void deleteMessage(ChatMessage message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Message");
        alert.setHeaderText("Are you sure you want to delete this message?");
        alert.setContentText("This action cannot be undone.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (chatService.deleteMessage(message.getId(), currentUser.getId())) {
                    // Refresh messages to remove the deleted message
                    loadInitialMessages();
                } else {
                    showAlert("Error", "Failed to delete message.");
                }
            }
        });
    }

    private String getSenderDisplayName(ChatMessage message) {
        if (message.getSenderFullName() != null && !message.getSenderFullName().trim().isEmpty()) {
            return message.getSenderFullName();
        }
        return message.getSenderUsername();
    }

    private String getCurrentUserDisplayName() {
        if (currentUser.getFullName() != null && !currentUser.getFullName().trim().isEmpty()) {
            return currentUser.getFullName();
        }
        return currentUser.getUsername();
    }

    private void scrollToBottom() {
        Platform.runLater(() -> {
            chatScrollPane.setVvalue(1.0);
        });
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void cleanup() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }
        if (currentUser != null) {
            chatService.updateUserOnlineStatus(currentUser.getId(), false);
        }
    }
}