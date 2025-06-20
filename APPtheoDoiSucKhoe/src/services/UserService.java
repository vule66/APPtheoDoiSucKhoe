package services;

import database.DatabaseManager;
import models.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.UUID;

public class UserService {
    private Connection connection;

    public UserService() {
        this.connection = DatabaseManager.getInstance().getConnection();
    }

    public User authenticateUser(String username, String password) {
        try {
            System.out.println("🔐 Authenticating user: " + username);

            String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setString(2, "hash_" + password); // Simple hash

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setEmail(rs.getString("email"));
                user.setFullName(rs.getString("full_name"));
                user.setAge(rs.getInt("age"));
                user.setGender(rs.getString("gender"));
                user.setHeight(rs.getDouble("height"));

                // Update last login
                updateLastLogin(user.getId());

                rs.close();
                stmt.close();

                System.out.println("✅ Authentication successful for: " + username);
                return user;
            }

            rs.close();
            stmt.close();
            System.out.println("❌ Authentication failed for: " + username);

        } catch (SQLException e) {
            System.out.println("❌ Database error during authentication: " + e.getMessage());
            e.printStackTrace();
        }

        return null; // Authentication failed
    }

    public boolean registerUser(User user, String password) {
        try {
            System.out.println("📝 Registering user: " + user.getUsername());

            String sql = """
                INSERT INTO users (username, email, password, full_name, age, gender, height) 
                VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, "hash_" + password);
            stmt.setString(4, user.getFullName());
            stmt.setInt(5, user.getAge());
            stmt.setString(6, user.getGender());
            stmt.setDouble(7, user.getHeight());

            int result = stmt.executeUpdate();
            stmt.close();

            if (result > 0) {
                System.out.println("✅ User registered successfully: " + user.getUsername());
                return true;
            } else {
                System.out.println("❌ Failed to register user: " + user.getUsername());
                return false;
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
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, username);

            ResultSet rs = stmt.executeQuery();
            rs.next();
            boolean exists = rs.getInt(1) > 0;

            rs.close();
            stmt.close();

            return exists;

        } catch (SQLException e) {
            System.out.println("❌ Error checking username: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean isEmailExists(String email) {
        try {
            String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, email);

            ResultSet rs = stmt.executeQuery();
            rs.next();
            boolean exists = rs.getInt(1) > 0;

            rs.close();
            stmt.close();

            return exists;

        } catch (SQLException e) {
            System.out.println("❌ Error checking email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public String generatePasswordResetToken(String email) {
        try {
            // Check if user exists
            String checkUserSql = "SELECT id FROM users WHERE email = ?";
            PreparedStatement checkStmt = connection.prepareStatement(checkUserSql);
            checkStmt.setString(1, email);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                int userId = rs.getInt("id");
                rs.close();
                checkStmt.close();

                // Generate token
                String token = UUID.randomUUID().toString();

                // Insert token with 1 hour expiry
                String insertTokenSql = """
                    INSERT INTO password_reset_tokens (user_id, token, expires_at) 
                    VALUES (?, ?, datetime('now', '+1 hour'))
                """;
                PreparedStatement insertStmt = connection.prepareStatement(insertTokenSql);
                insertStmt.setInt(1, userId);
                insertStmt.setString(2, token);
                insertStmt.executeUpdate();
                insertStmt.close();

                System.out.println("✅ Password reset token generated for: " + email);
                return token;
            }

            rs.close();
            checkStmt.close();

        } catch (SQLException e) {
            System.out.println("❌ Error generating reset token: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    public boolean resetPassword(String token, String newPassword) {
        try {
            // Check if token is valid and not expired
            String checkTokenSql = """
                SELECT user_id FROM password_reset_tokens 
                WHERE token = ? AND expires_at > datetime('now') AND used = FALSE
            """;
            PreparedStatement checkStmt = connection.prepareStatement(checkTokenSql);
            checkStmt.setString(1, token);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                int userId = rs.getInt("user_id");
                rs.close();
                checkStmt.close();

                // Update password
                String updatePasswordSql = "UPDATE users SET password = ? WHERE id = ?";
                PreparedStatement updateStmt = connection.prepareStatement(updatePasswordSql);
                updateStmt.setString(1, "hash_" + newPassword);
                updateStmt.setInt(2, userId);
                updateStmt.executeUpdate();
                updateStmt.close();

                // Mark token as used
                String markUsedSql = "UPDATE password_reset_tokens SET used = TRUE WHERE token = ?";
                PreparedStatement markStmt = connection.prepareStatement(markUsedSql);
                markStmt.setString(1, token);
                markStmt.executeUpdate();
                markStmt.close();

                System.out.println("✅ Password reset successfully for user ID: " + userId);
                return true;
            }

            rs.close();
            checkStmt.close();

        } catch (SQLException e) {
            System.out.println("❌ Error resetting password: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    private void updateLastLogin(int userId) {
        try {
            String sql = "UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE id = ?";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, userId);
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            System.out.println("❌ Error updating last login: " + e.getMessage());
            e.printStackTrace();
        }
    }
}