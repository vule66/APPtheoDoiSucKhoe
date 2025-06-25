package server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {
    private static final int PORT = 9876;
    static final Set<ClientHandler> clients = new HashSet<>();
    private static ExecutorService pool = Executors.newFixedThreadPool(10);
    private static final Object LOCK = new Object();

    private static final List<String> messageHistory = new ArrayList<>(50);

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Máy chủ đã khởi động trên cổng " + PORT);

        try {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Người dùng mới kết nối: " + clientSocket.getInetAddress().getHostAddress());

                ClientHandler clientHandler = new ClientHandler(clientSocket);
                pool.execute(clientHandler);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            serverSocket.close();
        }
    }

    public static synchronized void addClient(ClientHandler clientHandler) {
        if (clientHandler == null || clientHandler.getUsername() == null) {
            System.out.println("Không thể thêm client không hợp lệ");
            return;
        }

        String username = clientHandler.getUsername();
        removeClientByUsername(username);

        clients.add(clientHandler);
        System.out.println("Đã thêm client: " + username + ", hiện có " + clients.size() + " người dùng online");
        broadcastUserList();
    }

    public static synchronized void removeClientByUsername(String username) {
        if (username == null) return;

        Iterator<ClientHandler> iterator = clients.iterator();
        while (iterator.hasNext()) {
            ClientHandler client = iterator.next();
            if (username.equals(client.getUsername())) {
                System.out.println("Xóa client cũ: " + username);
                try {
                    client.disconnect();
                } catch (Exception e) {
                    System.out.println("Lỗi khi đóng kết nối cũ: " + e.getMessage());
                }
                iterator.remove();
            }
        }
    }

    // Sửa phương thức removeClient
    public static synchronized void removeClient(ClientHandler client) {
        if (client == null) return;

        boolean removed = clients.remove(client);
        if (removed) {
            System.out.println("Đã xóa client: " + client.getUsername() + ", còn lại " + clients.size() + " người dùng online");
            // Cập nhật danh sách người dùng
            broadcastUserList();
        }
    }

    // Sửa phương thức broadcastUserList
    public static void broadcastUserList() {
        List<String> users = getOnlineUsers();

        if (users.isEmpty()) {
            System.out.println("Không có người dùng online");
            return;
        }

        StringBuilder userList = new StringBuilder("USERS:");
        for (String user : users) {
            if (user != null && !user.trim().isEmpty()) {
                userList.append(user).append(",");
            }
        }

        String userListMessage = userList.toString();
        System.out.println("Phát sóng danh sách người dùng: " + userListMessage);

        synchronized (clients) {
            for (ClientHandler client : new HashSet<>(clients)) {
                try {
                    client.sendMessage(userListMessage);
                } catch (Exception e) {
                    System.out.println("Lỗi khi gửi danh sách người dùng đến " + client.getUsername() + ": " + e.getMessage());
                    removeClient(client);
                }
            }
        }
    }

    public static void broadcastMessage(String message, ClientHandler sender) {
        // Lưu tin nhắn vào lịch sử (nếu không phải tin nhắn hệ thống về danh sách người dùng)
        if (!message.startsWith("USERS:")) {
            addToMessageHistory(message);
        }

        System.out.println("Phát sóng tin nhắn: " + message);

        synchronized (LOCK) {
            for (ClientHandler client : clients) {
                try {
                    if (client != sender) {
                        client.sendMessage(message);
                    }
                } catch (Exception e) {
                    System.out.println("Lỗi khi gửi tin nhắn: " + e.getMessage());
                }
            }
        }
    }

    // Thêm tin nhắn vào lịch sử, giới hạn kích thước
    private static synchronized void addToMessageHistory(String message) {
        messageHistory.add(message);
        if (messageHistory.size() > 50) {
            messageHistory.remove(0);
        }
    }

    // Gửi lịch sử tin nhắn cho client mới
    public static void sendMessageHistory(ClientHandler client) {
        for (String message : messageHistory) {
            client.sendMessage(message);
        }
    }


    // Lấy danh sách người dùng đang online
    public static synchronized List<String> getOnlineUsers() {
        List<String> users = new ArrayList<>();
        for (ClientHandler client : clients) {
            if (client.getUsername() != null) {
                users.add(client.getUsername());
            }
        }
        return users;
    }
}