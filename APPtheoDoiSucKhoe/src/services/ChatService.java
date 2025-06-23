package services;

import database.DatabaseManager;
import models.ChatMessage;
import models.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ChatService {
    private final Connection connection;

    public ChatService() {
        this.connection = DatabaseManager.getInstance().getConnection();
        createChatTables();
    }

    private void createChatTables() {
        try {
            Statement stmt = connection.createStatement();

            // Create chat_messages table
            String createChatTable = """
                CREATE TABLE IF NOT EXISTS chat_messages (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    sender_id INTEGER NOT NULL,
                    content TEXT NOT NULL,
                    message_type VARCHAR(20) DEFAULT 'TEXT',
                    attachment_path TEXT,
                    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                    is_edited BOOLEAN DEFAULT FALSE,
                    edited_at DATETIME,
                    is_deleted BOOLEAN DEFAULT FALSE,
                    deleted_at DATETIME,
                    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE
                )
            """;

            // Create user_online_status table
            String createOnlineStatusTable = """
                CREATE TABLE IF NOT EXISTS user_online_status (
                    user_id INTEGER PRIMARY KEY,
                    is_online BOOLEAN DEFAULT FALSE,
                    last_seen DATETIME DEFAULT CURRENT_TIMESTAMP,
                    status_message TEXT,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                )
            """;

            // Create chat_room_members table (for future group chat feature)
            String createChatRoomTable = """
                CREATE TABLE IF NOT EXISTS chat_rooms (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    room_name VARCHAR(100) NOT NULL,
                    room_description TEXT,
                    is_public BOOLEAN DEFAULT TRUE,
                    created_by INTEGER NOT NULL,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (created_by) REFERENCES users(id)
                )
            """;

            stmt.execute(createChatTable);
            stmt.execute(createOnlineStatusTable);
            stmt.execute(createChatRoomTable);

            // Create indexes for better performance
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_chat_timestamp ON chat_messages(timestamp DESC)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_chat_sender ON chat_messages(sender_id)");

            stmt.close();
            System.out.println("✅ Chat tables created successfully!");

        } catch (SQLException e) {
            System.out.println("❌ Error creating chat tables: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean sendMessage(ChatMessage message) {
        try {
            String sql = """
                INSERT INTO chat_messages (sender_id, content, message_type, attachment_path, timestamp)
                VALUES (?, ?, ?, ?, ?)
            """;

            try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, message.getSenderId());
                stmt.setString(2, message.getContent());
                stmt.setString(3, message.getMessageType());
                stmt.setString(4, message.getAttachmentPath());
                stmt.setTimestamp(5, Timestamp.valueOf(message.getTimestamp()));

                int result = stmt.executeUpdate();

                if (result > 0) {
                    try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            message.setId(generatedKeys.getInt(1));
                        }
                    }
                    System.out.println("✅ Message sent successfully by user ID: " + message.getSenderId());
                    return true;
                }
            }
        } catch (SQLException e) {
            System.out.println("❌ Error sending message: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public List<ChatMessage> getRecentMessages(int limit) {
        List<ChatMessage> messages = new ArrayList<>();

        try {
            String sql = """
                SELECT cm.*, u.username, u.full_name
                FROM chat_messages cm
                JOIN users u ON cm.sender_id = u.id
                WHERE cm.is_deleted = FALSE
                ORDER BY cm.timestamp DESC
                LIMIT ?
            """;

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setInt(1, limit);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        ChatMessage message = mapResultSetToChatMessage(rs);
                        messages.add(0, message); // Add to beginning to maintain chronological order
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("❌ Error getting recent messages: " + e.getMessage());
            e.printStackTrace();
        }

        return messages;
    }

    public List<ChatMessage> getMessagesAfter(LocalDateTime timestamp) {
        List<ChatMessage> messages = new ArrayList<>();

        try {
            String sql = """
                SELECT cm.*, u.username, u.full_name
                FROM chat_messages cm
                JOIN users u ON cm.sender_id = u.id
                WHERE cm.timestamp > ? AND cm.is_deleted = FALSE
                ORDER BY cm.timestamp ASC
            """;

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setTimestamp(1, Timestamp.valueOf(timestamp));

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        ChatMessage message = mapResultSetToChatMessage(rs);
                        messages.add(message);
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("❌ Error getting messages after timestamp: " + e.getMessage());
            e.printStackTrace();
        }

        return messages;
    }

    public boolean editMessage(int messageId, String newContent, int userId) {
        try {
            String sql = """
                UPDATE chat_messages 
                SET content = ?, is_edited = TRUE, edited_at = CURRENT_TIMESTAMP
                WHERE id = ? AND sender_id = ? AND is_deleted = FALSE
            """;

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, newContent);
                stmt.setInt(2, messageId);
                stmt.setInt(3, userId);

                int result = stmt.executeUpdate();
                return result > 0;
            }
        } catch (SQLException e) {
            System.out.println("❌ Error editing message: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteMessage(int messageId, int userId) {
        try {
            String sql = """
                UPDATE chat_messages 
                SET is_deleted = TRUE, deleted_at = CURRENT_TIMESTAMP
                WHERE id = ? AND sender_id = ? AND is_deleted = FALSE
            """;

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setInt(1, messageId);
                stmt.setInt(2, userId);

                int result = stmt.executeUpdate();
                return result > 0;
            }
        } catch (SQLException e) {
            System.out.println("❌ Error deleting message: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public void updateUserOnlineStatus(int userId, boolean isOnline) {
        try {
            String sql = """
                INSERT OR REPLACE INTO user_online_status (user_id, is_online, last_seen)
                VALUES (?, ?, CURRENT_TIMESTAMP)
            """;

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                stmt.setBoolean(2, isOnline);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.out.println("❌ Error updating online status: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<User> getOnlineUsers() {
        List<User> onlineUsers = new ArrayList<>();

        try {
            String sql = """
                SELECT u.*, uos.last_seen
                FROM users u
                JOIN user_online_status uos ON u.id = uos.user_id
                WHERE uos.is_online = TRUE
                ORDER BY u.username
            """;

            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        User user = new User();
                        user.setId(rs.getInt("id"));
                        user.setUsername(rs.getString("username"));
                        user.setFullName(rs.getString("full_name"));
                        user.setEmail(rs.getString("email"));
                        onlineUsers.add(user);
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("❌ Error getting online users: " + e.getMessage());
            e.printStackTrace();
        }

        return onlineUsers;
    }

    private ChatMessage mapResultSetToChatMessage(ResultSet rs) throws SQLException {
        ChatMessage message = new ChatMessage();
        message.setId(rs.getInt("id"));
        message.setSenderId(rs.getInt("sender_id"));
        message.setSenderUsername(rs.getString("username"));
        message.setSenderFullName(rs.getString("full_name"));
        message.setContent(rs.getString("content"));
        message.setMessageType(rs.getString("message_type"));
        message.setAttachmentPath(rs.getString("attachment_path"));

        Timestamp timestamp = rs.getTimestamp("timestamp");
        if (timestamp != null) {
            message.setTimestamp(timestamp.toLocalDateTime());
        }

        message.setEdited(rs.getBoolean("is_edited"));

        Timestamp editedAt = rs.getTimestamp("edited_at");
        if (editedAt != null) {
            message.setEditedAt(editedAt.toLocalDateTime());
        }

        return message;
    }
}