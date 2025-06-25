package services;

import at.favre.lib.crypto.bcrypt.BCrypt;
import database.DatabaseManager;
import models.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class UserService {
    private final Connection connection;

    public UserService() {
        this.connection = DatabaseManager.getInstance().getConnection();
    }

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
            System.out.println("Registering user: " + user.getUsername());

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
                    System.out.println("User registered successfully: " + user.getUsername());
                    return true;
                } else {
                    System.out.println("Failed to register user: " + user.getUsername());
                    return false;
                }
            }

        } catch (SQLException e) {
            System.out.println("Database error during registration: " + e.getMessage());
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
            System.out.println("Error checking username: " + e.getMessage());
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
            System.out.println("Error checking email: " + e.getMessage());
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
            System.out.println("Error updating last login: " + e.getMessage());
            e.printStackTrace();
        }
    }
}