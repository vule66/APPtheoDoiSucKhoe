package database;

import java.sql.*;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:health_tracker.db";
    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() {
        try {
            System.out.println("🔄 Initializing DatabaseManager...");

            // Load SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");
            System.out.println("✅ SQLite JDBC driver loaded successfully!");

            connection = DriverManager.getConnection(DB_URL);
            System.out.println("✅ Database connection established: " + DB_URL);

            createTables();
            System.out.println("✅ Database initialized successfully!");

        } catch (Exception e) {
            System.out.println("❌ Database initialization failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

    private void createTables() {
        try {
            Statement stmt = connection.createStatement();

            // Users table
            String createUsersTable = """
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username VARCHAR(50) UNIQUE NOT NULL,
                    email VARCHAR(100) UNIQUE NOT NULL,
                    password VARCHAR(255) NOT NULL,
                    full_name VARCHAR(100),
                    age INTEGER,
                    gender VARCHAR(10),
                    height REAL,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    last_login DATETIME
                )
            """;

            // Health data table
            String createHealthDataTable = """
                CREATE TABLE IF NOT EXISTS health_data (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    date DATE NOT NULL,
                    steps INTEGER DEFAULT 0,
                    heart_rate INTEGER DEFAULT 0,
                    calories INTEGER DEFAULT 0,
                    sleep_minutes INTEGER DEFAULT 0,
                    water_intake REAL DEFAULT 0,
                    weight REAL DEFAULT 0,
                    systolic_bp INTEGER DEFAULT 0,
                    diastolic_bp INTEGER DEFAULT 0,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (user_id) REFERENCES users(id),
                    UNIQUE(user_id, date)
                )
            """;

            // Exercise table
            String createExerciseTable = """
                CREATE TABLE IF NOT EXISTS exercises (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    exercise_type VARCHAR(50) NOT NULL,
                    duration_minutes INTEGER NOT NULL,
                    calories_burned INTEGER DEFAULT 0,
                    exercise_date DATE NOT NULL,
                    notes TEXT,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (user_id) REFERENCES users(id)
                )
            """;

            // Password reset tokens table
            String createPasswordResetTable = """
                CREATE TABLE IF NOT EXISTS password_reset_tokens (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    token VARCHAR(255) NOT NULL,
                    expires_at DATETIME NOT NULL,
                    used BOOLEAN DEFAULT FALSE,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (user_id) REFERENCES users(id)
                )
            """;

            stmt.execute(createUsersTable);
            stmt.execute(createHealthDataTable);
            stmt.execute(createExerciseTable);
            stmt.execute(createPasswordResetTable);

            // Insert default user for testing
            insertDefaultUser();

            stmt.close();
            System.out.println("✅ Tables created successfully!");

        } catch (SQLException e) {
            System.out.println("❌ Error creating tables: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void insertDefaultUser() {
        try {
            String checkUser = "SELECT COUNT(*) FROM users WHERE username = ?";
            PreparedStatement checkStmt = connection.prepareStatement(checkUser);
            checkStmt.setString(1, "vule66");
            ResultSet rs = checkStmt.executeQuery();
            rs.next();

            if (rs.getInt(1) == 0) {
                String insertUser = """
                    INSERT INTO users (username, email, password, full_name, age, gender, height) 
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
                PreparedStatement stmt = connection.prepareStatement(insertUser);
                stmt.setString(1, "vule66");
                stmt.setString(2, "vule66@example.com");
                stmt.setString(3, hashPassword("123456")); // Simple hash
                stmt.setString(4, "Vu Le");
                stmt.setInt(5, 25);
                stmt.setString(6, "Male");
                stmt.setDouble(7, 175.0);
                stmt.executeUpdate();
                stmt.close();
                System.out.println("✅ Default user created: vule66/123456");
            } else {
                System.out.println("ℹ️ Default user already exists");
            }

            checkStmt.close();
            rs.close();

        } catch (SQLException e) {
            System.out.println("❌ Error inserting default user: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String hashPassword(String password) {
        // Simple hash for demo - use BCrypt in production
        return "hash_" + password;
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("✅ Database connection closed");
            }
        } catch (SQLException e) {
            System.out.println("❌ Error closing connection: " + e.getMessage());
            e.printStackTrace();
        }
    }
}