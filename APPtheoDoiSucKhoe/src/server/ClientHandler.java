package server;

import java.io.*;
import java.net.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;
    public static final Set<ClientHandler> clients = new HashSet<>();
    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            // Lấy tên người dùng khi kết nối lần đầu
            username = in.readLine();
            System.out.println("👤 Đã nhận tên người dùng: " + username);

            // Đảm bảo username không null hoặc rỗng
            if (username == null || username.trim().isEmpty()) {
                System.out.println("⚠️ Tên người dùng không hợp lệ, đóng kết nối");
                return;
            }

            // Thêm client này vào danh sách
            ChatServer.addClient(this);

            // Gửi lịch sử tin nhắn cho người dùng mới
            System.out.println("📚 Gửi lịch sử tin nhắn cho " + username);
            ChatServer.sendMessageHistory(this);

            // Thông báo cho mọi người biết có người dùng mới tham gia
            ChatServer.broadcastMessage(username + " đã tham gia cuộc trò chuyện!", this);

            String message;
            while ((message = in.readLine()) != null) {
                if (message.equalsIgnoreCase("EXIT")) {
                    System.out.println("👋 Người dùng " + username + " yêu cầu thoát");
                    break;
                }
                System.out.println("📨 Nhận tin nhắn từ " + username + ": " + message);
                ChatServer.broadcastMessage(username + ": " + message, this);
            }
        } catch (IOException e) {
            System.out.println("❌ Lỗi kết nối với người dùng " + (username != null ? username : "không xác định") + ": " + e.getMessage());
        } finally {
            try {
                System.out.println("🔌 Đóng kết nối với người dùng " + (username != null ? username : "không xác định"));
                disconnect();

                ChatServer.removeClient(this);

                // Chỉ gửi thông báo nếu username không null
                if (username != null) {
                    // Thông báo rằng người dùng đã rời đi
                    ChatServer.broadcastMessage(username + " đã rời khỏi cuộc trò chuyện!", null);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMessage(String message) {
        try {
            if (out != null) {
                out.println(message);
                // Kiểm tra lỗi khi gửi
                if (out.checkError()) {
                    throw new IOException("Lỗi khi gửi tin nhắn");
                }
            }
        } catch (IOException e) {
            System.out.println("❌ Không thể gửi tin nhắn đến " + username + ": " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void disconnect() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
            System.out.println("✅ Đã đóng kết nối với " + username);
        } catch (IOException e) {
            System.out.println("⚠️ Lỗi khi đóng kết nối: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public String getUsername() {
        return username;
    }
}