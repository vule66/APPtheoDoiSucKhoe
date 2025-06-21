package services;

import at.favre.lib.crypto.bcrypt.BCrypt;
import database.DatabaseManager;
import models.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public class UserService {
    private final Connection connection;

    public UserService() {
        this.connection = DatabaseManager.getInstance().getConnection();
    }

    // <<< PHƯƠNG THỨC HỖ TRỢ ĐỂ TRÁNH LẶP CODE >>>
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setFullName(rs.getString("full_name"));
        if (rs.getObject("age") != null) {
            user.setAge(rs.getInt("age"));
        }
        user.setGender(rs.getString("gender"));
        if (rs.getObject("height") != null) {
            user.setHeight(rs.getDouble("height"));
        }
        return user;
    }

    public User authenticateUser(String username, String password) {
        try {
            String sql = "SELECT * FROM users WHERE username = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, username);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String hashedPassword = rs.getString("password");
                        BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), hashedPassword);
                        if (result.verified) {
                            User user = mapResultSetToUser(rs); // Tái sử dụng
                            updateLastLogin(user.getId());
                            return user;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // <<< PHƯƠNG THỨC ĐƯỢC BỔ SUNG ĐỂ SỬA LỖI >>>
    public Optional<User> findUserById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs)); // Tái sử dụng
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public boolean registerUser(User user, String password) {
        try {
            System.out.println("📝 Registering user: " + user.getUsername());

            // Hash the password with BCrypt
            String hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray());

            String sql = """
                INSERT INTO users (username, email, password, full_name, age, gender, height)
                VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, user.getUsername());
                stmt.setString(2, user.getEmail());
                stmt.setString(3, hashedPassword); // Store the hashed password
                stmt.setString(4, user.getFullName());
                stmt.setInt(5, user.getAge());
                stmt.setString(6, user.getGender());
                stmt.setDouble(7, user.getHeight());

                int result = stmt.executeUpdate();

                if (result > 0) {
                    System.out.println("✅ User registered successfully: " + user.getUsername());
                    return true;
                } else {
                    System.out.println("❌ Failed to register user: " + user.getUsername());
                    return false;
                }
            }

        } catch (SQLException e) {
            System.out.println("❌ Database error during registration: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean isUsernameExists(String username) {
        try {
            String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, username);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1) > 0;
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("❌ Error checking username: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean isEmailExists(String email) {
        try {
            String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, email);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1) > 0;
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("❌ Error checking email: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public String generatePasswordResetToken(String email) {
        try {
            String checkUserSql = "SELECT id FROM users WHERE email = ?";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkUserSql)) {
                checkStmt.setString(1, email);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        int userId = rs.getInt("id");
                        String token = UUID.randomUUID().toString();

                        String insertTokenSql = """
                            INSERT INTO password_reset_tokens (user_id, token, expires_at)
                            VALUES (?, ?, datetime('now', '+1 hour'))
                        """;
                        try (PreparedStatement insertStmt = connection.prepareStatement(insertTokenSql)) {
                            insertStmt.setInt(1, userId);
                            insertStmt.setString(2, token);
                            insertStmt.executeUpdate();
                        }

                        System.out.println("✅ Password reset token generated for: " + email);
                        return token;
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("❌ Error generating reset token: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public boolean resetPassword(String token, String newPassword) {
        try {
            String checkTokenSql = """
                SELECT user_id FROM password_reset_tokens
                WHERE token = ? AND expires_at > datetime('now') AND used = FALSE
            """;
            try (PreparedStatement checkStmt = connection.prepareStatement(checkTokenSql)) {
                checkStmt.setString(1, token);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        int userId = rs.getInt("user_id");
                        String hashedPassword = BCrypt.withDefaults().hashToString(12, newPassword.toCharArray());

                        String updatePasswordSql = "UPDATE users SET password = ? WHERE id = ?";
                        try (PreparedStatement updateStmt = connection.prepareStatement(updatePasswordSql)) {
                            updateStmt.setString(1, hashedPassword);
                            updateStmt.setInt(2, userId);
                            updateStmt.executeUpdate();
                        }

                        String markUsedSql = "UPDATE password_reset_tokens SET used = TRUE WHERE token = ?";
                        try (PreparedStatement markStmt = connection.prepareStatement(markUsedSql)) {
                            markStmt.setString(1, token);
                            markStmt.executeUpdate();
                        }

                        System.out.println("✅ Password reset successfully for user ID: " + userId);
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("❌ Error resetting password: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    private void updateLastLogin(int userId) {
        try {
            String sql = "UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.out.println("❌ Error updating last login: " + e.getMessage());
            e.printStackTrace();
        }
    }
}