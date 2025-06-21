package services;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Lớp tiện ích trung tâm, chịu trách nhiệm duy nhất cho việc
 * tạo kết nối đến cơ sở dữ liệu SQLite.
 */
public class DatabaseService {

    // Đường dẫn đến tệp database. SQLite sẽ tự động tạo tệp này nếu nó không tồn tại.
    private static final String DB_URL = "jdbc:sqlite:health_tracker.db";

    /**
     * Phương thức tĩnh (static) để các lớp khác có thể gọi trực tiếp
     * mà không cần tạo đối tượng DatabaseService.
     * @return một đối tượng Connection đã sẵn sàng để sử dụng.
     */
    public static Connection connect() {
        Connection conn = null;
        try {
            // Class.forName("org.sqlite.JDBC"); // Dòng này thường không cần thiết với các driver JDBC hiện đại
            conn = DriverManager.getConnection(DB_URL);
        } catch (SQLException e) {
            System.err.println("Database connection error: " + e.getMessage());
        }
        return conn;
    }

    // Bạn có thể đã có phương thức này từ trước, không sao cả.
    // Nó dùng để khởi tạo các bảng ban đầu.
    public static void initializeDatabase() {
        String userTableSql = "CREATE TABLE IF NOT EXISTS users ("
                + " user_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " username TEXT NOT NULL UNIQUE,"
                + " password TEXT NOT NULL,"
                + " full_name TEXT NOT NULL"
                + ");";

        String healthEntryTableSql = "CREATE TABLE IF NOT EXISTS health_entries ("
                + " entry_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " user_id INTEGER NOT NULL,"
                + " entry_date TEXT NOT NULL,"
                + " weight_kg REAL,"
                + " blood_pressure TEXT,"
                + " hours_slept REAL,"
                + " created_at TEXT NOT NULL DEFAULT (datetime('now','localtime')),"
                + " FOREIGN KEY (user_id) REFERENCES users (user_id)"
                + ");";

        String indexSql = "CREATE UNIQUE INDEX IF NOT EXISTS idx_user_date ON health_entries (user_id, entry_date);";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            // thực thi các lệnh tạo bảng
            stmt.execute(userTableSql);
            stmt.execute(healthEntryTableSql);
            stmt.execute(indexSql);
        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
        }
    }
}