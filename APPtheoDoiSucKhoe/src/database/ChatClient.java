package database;

import java.io.*;
import java.net.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;

public class ChatClient {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private TextArea chatArea;
    private ListView<String> userListView;
    private ObservableList<String> onlineUsers;
    private String username;
    private boolean connected = false;

    public ChatClient(String username, TextArea chatArea, ListView<String> userListView) {
        this.username = username;
        this.chatArea = chatArea;
        this.userListView = userListView;
        this.onlineUsers = FXCollections.observableArrayList();
        userListView.setItems(onlineUsers);
    }

    public boolean connect() {
        try {
            System.out.println("Đang kết nối đến máy chủ chat...");
            socket = new Socket("localhost", 9876);
            socket.setSoTimeout(30000); // Timeout 30 giây

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            System.out.println("Đã kết nối, gửi tên người dùng: " + username);
            // Gửi tên người dùng đến máy chủ
            out.println(username);

            // Bắt đầu một luồng để xử lý tin nhắn đến
            Thread listenerThread = new Thread(this::listenForMessages);
            listenerThread.setDaemon(true);
            listenerThread.start();

            connected = true;
            System.out.println("Kết nối thành công!");
            return true;
        } catch (IOException e) {
            System.out.println("Lỗi kết nối: " + e.getMessage());
            e.printStackTrace();
            connected = false;
            return false;
        }
    }

    private void listenForMessages() {
        try {
            String message;
            while (connected && (message = in.readLine()) != null) {
                final String finalMessage = message;
                System.out.println("Nhận tin nhắn: " + finalMessage);

                // Kiểm tra xem đây có phải là cập nhật danh sách người dùng không
                if (message.startsWith("USERS:")) {
                    String[] userArray = message.substring(6).split(",");
                    Platform.runLater(() -> {
                        onlineUsers.clear();
                        for (String user : userArray) {
                            if (user != null && !user.trim().isEmpty()) {
                                onlineUsers.add(user);
                            }
                        }
                        System.out.println("Danh sách người dùng trực tuyến: " + onlineUsers);
                    });
                } else {
                    // Tin nhắn chat thông thường
                    Platform.runLater(() -> {
                        chatArea.appendText(finalMessage + "\n");
                    });
                }
            }
        } catch (IOException e) {
            System.out.println("Lỗi đọc tin nhắn: " + e.getMessage());
            if (connected) {
                // Chỉ hiển thị lỗi nếu trước đó đang kết nối
                Platform.runLater(() -> {
                    chatArea.appendText("Đã mất kết nối khỏi máy chủ\n");
                });
                connected = false;
            }
        } finally {
            if (connected) {  // Chỉ disconnect nếu vẫn đang kết nối
                System.out.println("Luồng lắng nghe tin nhắn kết thúc, đóng kết nối...");
                disconnect();
            }
        }
    }
    public void sendMessage(String message) {
        if (!connected) {
            System.out.println("Không thể gửi tin nhắn: không có kết nối");
            Platform.runLater(() -> {
                chatArea.appendText("Không thể gửi tin nhắn: đã mất kết nối với máy chủ chat\n");
            });
            return;
        }

        if (socket == null || socket.isClosed()) {
            System.out.println("Không thể gửi tin nhắn: socket đã đóng");
            connected = false;
            Platform.runLater(() -> {
                chatArea.appendText("Không thể gửi tin nhắn: đã mất kết nối với máy chủ chat\n");
            });
            return;
        }

        System.out.println("Gửi tin nhắn: " + message);

        // Hiển thị tin nhắn của mình trong khung chat trước khi gửi đi
        Platform.runLater(() -> {
            chatArea.appendText(username + ": " + message + "\n");
        });

        // Gửi tin nhắn đến máy chủ
        out.println(message);

        // Kiểm tra lỗi
        if (out.checkError()) {
            System.out.println("Lỗi khi gửi tin nhắn");
            connected = false;
            Platform.runLater(() -> {
                chatArea.appendText("Không thể gửi tin nhắn: đã mất kết nối với máy chủ chat\n");
            });
        } else {
            System.out.println("Đã gửi tin nhắn thành công");
        }
    }

    public void disconnect() {
        if (!connected) {
            return; // Tránh đóng kết nối nhiều lần
        }

        System.out.println("🔌 Đóng kết nối chat cho " + username + "...");
        connected = false; // Đặt cờ này trước để tránh các cuộc gọi đệ quy

        try {
            if (out != null) {
                out.println("EXIT");
                out.flush(); // Đảm bảo lệnh EXIT được gửi
            }

            // Thêm thời gian trễ nhỏ để đảm bảo lệnh EXIT được xử lý
            try { Thread.sleep(200); } catch (InterruptedException e) {}

            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();

            System.out.println("✅ Đã đóng kết nối chat thành công cho " + username);

            // Xóa danh sách người dùng
            Platform.runLater(() -> {
                onlineUsers.clear();
                chatArea.appendText("Đã ngắt kết nối khỏi chat\n");
            });
        } catch (IOException e) {
            System.out.println("⚠️ Lỗi khi đóng kết nối: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public boolean isConnected() {
        return connected;
    }
}