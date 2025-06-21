package database;

import java.sql.*;
import java.util.UUID;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:health_tracker.db";
    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() {
        try {
            System.out.println("🔄 Initializing Enhanced DatabaseManager...");

            // Load SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");
            System.out.println("✅ SQLite JDBC driver loaded successfully!");

            connection = DriverManager.getConnection(DB_URL);

            // Enable foreign keys
            Statement stmt = connection.createStatement();
            stmt.execute("PRAGMA foreign_keys = ON;");
            stmt.close();

            System.out.println("✅ Database connection established: " + DB_URL);

            // Check if migration is needed
            checkAndMigrate();
            createTables();
            createIndexes();
            insertSampleData();
            System.out.println("✅ Enhanced Database initialized successfully!");

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

    private void checkAndMigrate() {
        try {
            // Check if old table structure exists
            DatabaseMetaData meta = connection.getMetaData();
            ResultSet rs = meta.getColumns(null, null, "health_data", "date");

            if (rs.next()) {
                // Old structure exists, migrate it
                System.out.println("🔄 Migrating old database structure...");
                Statement stmt = connection.createStatement();

                // Rename old column
                stmt.execute("ALTER TABLE health_data RENAME COLUMN date TO record_date");
                System.out.println("✅ Migrated 'date' column to 'record_date'");

                stmt.close();
            }
            rs.close();

        } catch (SQLException e) {
            // Column might not exist or already migrated
            System.out.println("ℹ️ Database migration check: " + e.getMessage());
        }
    }

    private void createTables() {
        try {
            Statement stmt = connection.createStatement();

            // Enhanced Users table - check if columns exist before adding
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
                    weight REAL,
                    activity_level VARCHAR(20) DEFAULT 'Moderate',
                    daily_calorie_goal INTEGER DEFAULT 2000,
                    daily_water_goal REAL DEFAULT 2.0,
                    daily_step_goal INTEGER DEFAULT 10000,
                    profile_image TEXT,
                    is_active BOOLEAN DEFAULT TRUE,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    last_login DATETIME
                )
            """;

            // Enhanced Health data table - use record_date consistently
            String createHealthDataTable = """
                CREATE TABLE IF NOT EXISTS health_data (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    record_date DATE NOT NULL,
                    steps INTEGER DEFAULT 0,
                    heart_rate INTEGER DEFAULT 0,
                    calories_burned INTEGER DEFAULT 0,
                    calories_consumed INTEGER DEFAULT 0,
                    sleep_hours REAL DEFAULT 0,
                    water_intake REAL DEFAULT 0,
                    weight REAL,
                    systolic_bp INTEGER,
                    diastolic_bp INTEGER,
                    mood_rating INTEGER,
                    energy_level INTEGER,
                    stress_level INTEGER,
                    notes TEXT,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                    UNIQUE(user_id, record_date)
                )
            """;

            // Enhanced Exercise table
            String createExerciseTable = """
                CREATE TABLE IF NOT EXISTS exercises (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    exercise_name VARCHAR(100) NOT NULL,
                    exercise_type VARCHAR(50) NOT NULL,
                    duration_minutes INTEGER NOT NULL,
                    calories_burned INTEGER DEFAULT 0,
                    intensity VARCHAR(20) DEFAULT 'Moderate',
                    exercise_date DATE NOT NULL,
                    start_time TIME,
                    end_time TIME,
                    notes TEXT,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                )
            """;

            // Goals table
            String createGoalsTable = """
                CREATE TABLE IF NOT EXISTS user_goals (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    goal_type VARCHAR(50) NOT NULL,
                    goal_description TEXT NOT NULL,
                    target_value REAL,
                    target_unit VARCHAR(20),
                    current_value REAL DEFAULT 0,
                    start_date DATE NOT NULL,
                    target_date DATE NOT NULL,
                    is_achieved BOOLEAN DEFAULT FALSE,
                    priority_level VARCHAR(10) DEFAULT 'Medium',
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    achieved_at DATETIME,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                )
            """;

            // Achievements table
            String createAchievementsTable = """
                CREATE TABLE IF NOT EXISTS achievements (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    achievement_name VARCHAR(100) NOT NULL UNIQUE,
                    description TEXT NOT NULL,
                    achievement_type VARCHAR(50) NOT NULL,
                    criteria_value REAL NOT NULL,
                    criteria_unit VARCHAR(20),
                    badge_icon VARCHAR(100),
                    points_reward INTEGER DEFAULT 0,
                    is_active BOOLEAN DEFAULT TRUE,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            """;

            // User achievements table
            String createUserAchievementsTable = """
                CREATE TABLE IF NOT EXISTS user_achievements (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    achievement_id INTEGER NOT NULL,
                    earned_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    progress_value REAL DEFAULT 0,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                    FOREIGN KEY (achievement_id) REFERENCES achievements(id) ON DELETE CASCADE,
                    UNIQUE(user_id, achievement_id)
                )
            """;

            // Password reset tokens table
            String createPasswordResetTable = """
                CREATE TABLE IF NOT EXISTS password_reset_tokens (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    token VARCHAR(255) UNIQUE NOT NULL,
                    expires_at DATETIME NOT NULL,
                    used BOOLEAN DEFAULT FALSE,
                    ip_address VARCHAR(45),
                    user_agent TEXT,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    used_at DATETIME,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                )
            """;

            // Execute all table creation statements
            stmt.execute(createUsersTable);
            stmt.execute(createHealthDataTable);
            stmt.execute(createExerciseTable);
            stmt.execute(createGoalsTable);
            stmt.execute(createAchievementsTable);
            stmt.execute(createUserAchievementsTable);
            stmt.execute(createPasswordResetTable);

            // Add new columns to existing users table if they don't exist
            addColumnIfNotExists("users", "weight", "REAL");
            addColumnIfNotExists("users", "activity_level", "VARCHAR(20) DEFAULT 'Moderate'");
            addColumnIfNotExists("users", "daily_calorie_goal", "INTEGER DEFAULT 2000");
            addColumnIfNotExists("users", "daily_water_goal", "REAL DEFAULT 2.0");
            addColumnIfNotExists("users", "daily_step_goal", "INTEGER DEFAULT 10000");
            addColumnIfNotExists("users", "profile_image", "TEXT");
            addColumnIfNotExists("users", "is_active", "BOOLEAN DEFAULT TRUE");
            addColumnIfNotExists("users", "updated_at", "DATETIME DEFAULT CURRENT_TIMESTAMP");

            stmt.close();
            System.out.println("✅ Enhanced tables created successfully!");

        } catch (SQLException e) {
            System.out.println("❌ Error creating tables: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void addColumnIfNotExists(String tableName, String columnName, String columnType) {
        try {
            DatabaseMetaData meta = connection.getMetaData();
            ResultSet rs = meta.getColumns(null, null, tableName, columnName);

            if (!rs.next()) {
                // Column doesn't exist, add it
                Statement stmt = connection.createStatement();
                stmt.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnType);
                stmt.close();
                System.out.println("✅ Added column " + columnName + " to " + tableName);
            }
            rs.close();

        } catch (SQLException e) {
            System.out.println("⚠️ Could not add column " + columnName + " to " + tableName + ": " + e.getMessage());
        }
    }

    private void createIndexes() {
        try {
            Statement stmt = connection.createStatement();

            // Indexes for better performance - check if columns exist first
            String[] indexes = {
                    "CREATE INDEX IF NOT EXISTS idx_users_username ON users(username)",
                    "CREATE INDEX IF NOT EXISTS idx_users_email ON users(email)",
                    "CREATE INDEX IF NOT EXISTS idx_health_data_user_date ON health_data(user_id, record_date)",
                    "CREATE INDEX IF NOT EXISTS idx_health_data_date ON health_data(record_date)",
                    "CREATE INDEX IF NOT EXISTS idx_exercises_user_date ON exercises(user_id, exercise_date)",
                    "CREATE INDEX IF NOT EXISTS idx_goals_user_type ON user_goals(user_id, goal_type)",
                    "CREATE INDEX IF NOT EXISTS idx_user_achievements_user ON user_achievements(user_id)"
            };

            for (String index : indexes) {
                try {
                    stmt.execute(index);
                } catch (SQLException e) {
                    System.out.println("⚠️ Could not create index: " + e.getMessage());
                }
            }

            stmt.close();
            System.out.println("✅ Database indexes created successfully!");

        } catch (SQLException e) {
            System.out.println("❌ Error creating indexes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void insertSampleData() {
        try {
            insertDefaultUser();
            insertSampleAchievements();
            insertSampleGoals();
            System.out.println("✅ Sample data inserted successfully!");

        } catch (Exception e) {
            System.out.println("❌ Error inserting sample data: " + e.getMessage());
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
                    INSERT INTO users (username, email, password, full_name, age, gender, height, weight, 
                                     activity_level, daily_calorie_goal, daily_water_goal, daily_step_goal) 
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
                PreparedStatement stmt = connection.prepareStatement(insertUser);
                stmt.setString(1, "vule66");
                stmt.setString(2, "vule66@example.com");
                stmt.setString(3, hashPassword("123456"));
                stmt.setString(4, "Vu Le");
                stmt.setInt(5, 25);
                stmt.setString(6, "Male");
                stmt.setDouble(7, 175.0);
                stmt.setDouble(8, 70.0);
                stmt.setString(9, "Active");
                stmt.setInt(10, 2500);
                stmt.setDouble(11, 3.0);
                stmt.setInt(12, 12000);
                stmt.executeUpdate();
                stmt.close();
                System.out.println("✅ Enhanced default user created: vule66/123456");
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

    private void insertSampleAchievements() {
        try {
            String checkAchievements = "SELECT COUNT(*) FROM achievements";
            PreparedStatement checkStmt = connection.prepareStatement(checkAchievements);
            ResultSet rs = checkStmt.executeQuery();
            rs.next();

            if (rs.getInt(1) == 0) {
                String insertAchievement = """
                    INSERT INTO achievements (achievement_name, description, achievement_type, criteria_value, criteria_unit, points_reward) 
                    VALUES (?, ?, ?, ?, ?, ?)
                """;

                PreparedStatement stmt = connection.prepareStatement(insertAchievement);

                // Steps achievements
                stmt.setString(1, "First Steps"); stmt.setString(2, "Take your first 1000 steps");
                stmt.setString(3, "Steps"); stmt.setDouble(4, 1000); stmt.setString(5, "steps"); stmt.setInt(6, 50);
                stmt.executeUpdate();

                stmt.setString(1, "Daily Walker"); stmt.setString(2, "Reach 10,000 steps in a day");
                stmt.setString(3, "Steps"); stmt.setDouble(4, 10000); stmt.setString(5, "steps"); stmt.setInt(6, 100);
                stmt.executeUpdate();

                stmt.setString(1, "Marathon Walker"); stmt.setString(2, "Reach 25,000 steps in a day");
                stmt.setString(3, "Steps"); stmt.setDouble(4, 25000); stmt.setString(5, "steps"); stmt.setInt(6, 200);
                stmt.executeUpdate();

                stmt.close();
                System.out.println("✅ Sample achievements inserted");
            }

            checkStmt.close();
            rs.close();

        } catch (SQLException e) {
            System.out.println("❌ Error inserting achievements: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void insertSampleGoals() {
        try {
            String checkGoals = "SELECT COUNT(*) FROM user_goals WHERE user_id = 1";
            PreparedStatement checkStmt = connection.prepareStatement(checkGoals);
            ResultSet rs = checkStmt.executeQuery();
            rs.next();

            if (rs.getInt(1) == 0) {
                String insertGoal = """
                    INSERT INTO user_goals (user_id, goal_type, goal_description, target_value, target_unit, start_date, target_date, priority_level) 
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

                PreparedStatement stmt = connection.prepareStatement(insertGoal);

                stmt.setInt(1, 1);
                stmt.setString(2, "Weight Loss");
                stmt.setString(3, "Lose 5kg in 3 months through healthy diet and exercise");
                stmt.setDouble(4, 65.0);
                stmt.setString(5, "kg");
                stmt.setString(6, "2025-06-20");
                stmt.setString(7, "2025-09-20");
                stmt.setString(8, "High");
                stmt.executeUpdate();

                stmt.close();
                System.out.println("✅ Sample goals inserted for default user");
            }

            checkStmt.close();
            rs.close();

        } catch (SQLException e) {
            System.out.println("❌ Error inserting sample goals: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String hashPassword(String password) {
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