package services;

import database.DatabaseManager;
import models.HealthEntry;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.time.Instant;
import java.time.ZoneId;

public class HealthEntryService {

    public void saveOrUpdateEntry(HealthEntry entry) throws SQLException {
        Connection conn = DatabaseManager.getInstance().getConnection();

        // Kiểm tra xem đã có bản ghi nào cho người dùng và ngày này chưa
        String checkSql = "SELECT id FROM health_data WHERE user_id = ? AND record_date = ?";
        PreparedStatement checkStmt = conn.prepareStatement(checkSql);
        checkStmt.setInt(1, entry.getUserId());
        checkStmt.setDate(2, java.sql.Date.valueOf(entry.getRecordDate()));
        ResultSet rs = checkStmt.executeQuery();

        boolean exists = rs.next();
        rs.close();
        checkStmt.close();

        PreparedStatement stmt = null;

        try {
            if (exists) {
                // UPDATE nếu đã tồn tại - bao gồm các trường mới
                String updateSql = "UPDATE health_data SET weight = ?, systolic_bp = ?, diastolic_bp = ?, " +
                        "steps = ?, heart_rate = ?, calories = ?, water_intake = ?, " +
                        "sleep_minutes = ?, updated_at = CURRENT_TIMESTAMP " +
                        "WHERE user_id = ? AND record_date = ?";
                stmt = conn.prepareStatement(updateSql);

                // Thiết lập các tham số
                int i = 1;
                stmt.setObject(i++, entry.getWeight());
                stmt.setObject(i++, entry.getSystolicBp());
                stmt.setObject(i++, entry.getDiastolicBp());
                stmt.setObject(i++, entry.getSteps());
                stmt.setObject(i++, entry.getHeartRate());
                stmt.setObject(i++, entry.getCalories());
                stmt.setObject(i++, entry.getWaterIntake());

                // Chuyển đổi giờ sang phút nếu có giá trị
                Integer sleepMinutes = null;
                if (entry.getSleepHours() != null) {
                    sleepMinutes = (int)(entry.getSleepHours() * 60);
                }
                stmt.setObject(i++, sleepMinutes);

                stmt.setInt(i++, entry.getUserId());
                stmt.setDate(i++, java.sql.Date.valueOf(entry.getRecordDate()));

            } else {
                // INSERT nếu chưa tồn tại - bao gồm các trường mới
                String insertSql = "INSERT INTO health_data (user_id, record_date, weight, systolic_bp, diastolic_bp, " +
                        "steps, heart_rate, calories, water_intake, sleep_minutes, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
                stmt = conn.prepareStatement(insertSql);

                // Thiết lập các tham số
                int i = 1;
                stmt.setInt(i++, entry.getUserId());
                stmt.setDate(i++, java.sql.Date.valueOf(entry.getRecordDate()));
                stmt.setObject(i++, entry.getWeight());
                stmt.setObject(i++, entry.getSystolicBp());
                stmt.setObject(i++, entry.getDiastolicBp());
                stmt.setObject(i++, entry.getSteps());
                stmt.setObject(i++, entry.getHeartRate());
                stmt.setObject(i++, entry.getCalories());
                stmt.setObject(i++, entry.getWaterIntake());

                // Chuyển đổi giờ sang phút nếu có giá trị
                Integer sleepMinutes = null;
                if (entry.getSleepHours() != null) {
                    sleepMinutes = (int)(entry.getSleepHours() * 60);
                }
                stmt.setObject(i++, sleepMinutes);
            }

            // Thực hiện câu lệnh SQL
            int rowsAffected = stmt.executeUpdate();
            System.out.println("Rows affected: " + rowsAffected);

        } finally {
            if (stmt != null) stmt.close();
        }
    }

    public Optional<HealthEntry> findEntryByUserIdAndDate(int userId, LocalDate date) throws SQLException {
        Connection conn = DatabaseManager.getInstance().getConnection();
        String sql = "SELECT * FROM health_data WHERE user_id = ? AND record_date = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setDate(2, java.sql.Date.valueOf(date));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // Sử dụng phương thức chung để lấy LocalDate từ ResultSet
                    LocalDate recordDate = getLocalDateFromResultSet(rs, "record_date");

                    HealthEntry entry = new HealthEntry(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            recordDate,
                            rs.getObject("weight") != null ? rs.getDouble("weight") : null,
                            rs.getObject("systolic_bp") != null ? rs.getInt("systolic_bp") : null,
                            rs.getObject("diastolic_bp") != null ? rs.getInt("diastolic_bp") : null,
                            rs.getObject("sleep_minutes") != null ? rs.getDouble("sleep_minutes") / 60.0 : null,
                            rs.getObject("steps") != null ? rs.getInt("steps") : null,
                            rs.getObject("heart_rate") != null ? rs.getInt("heart_rate") : null,
                            rs.getObject("calories") != null ? rs.getInt("calories") : null,
                            rs.getObject("water_intake") != null ? rs.getDouble("water_intake") : null
                    );
                    return Optional.of(entry);
                }
            }
        }
        return Optional.empty();
    }
    public List<HealthEntry> findEntriesByUserIdAndDateRange(int userId, LocalDate startDate, LocalDate endDate) throws SQLException {
        List<HealthEntry> entries = new ArrayList<>();
        Connection conn = DatabaseManager.getInstance().getConnection();
        String sql = "SELECT * FROM health_data WHERE user_id = ? AND record_date BETWEEN ? AND ? ORDER BY record_date";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setDate(2, java.sql.Date.valueOf(startDate));
            stmt.setDate(3, java.sql.Date.valueOf(endDate));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    LocalDate recordDate = getLocalDateFromResultSet(rs, "record_date");

                    HealthEntry entry = new HealthEntry(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            recordDate,
                            rs.getObject("weight") != null ? rs.getDouble("weight") : null,
                            rs.getObject("systolic_bp") != null ? rs.getInt("systolic_bp") : null,
                            rs.getObject("diastolic_bp") != null ? rs.getInt("diastolic_bp") : null,
                            rs.getObject("sleep_minutes") != null ? rs.getDouble("sleep_minutes") / 60.0 : null,
                            rs.getObject("steps") != null ? rs.getInt("steps") : null,
                            rs.getObject("heart_rate") != null ? rs.getInt("heart_rate") : null,
                            rs.getObject("calories") != null ? rs.getInt("calories") : null,
                            rs.getObject("water_intake") != null ? rs.getDouble("water_intake") : null
                    );
                    entries.add(entry);
                }
            }
        }
        return entries;
    }
    // Thêm phương thức xử lý đa dạng định dạng ngày tháng
    private LocalDate getLocalDateFromResultSet(ResultSet rs, String columnName) throws SQLException {
        try {
            // Thử lấy dưới dạng Date
            java.sql.Date date = rs.getDate(columnName);
            if (date != null) {
                return date.toLocalDate();
            }
        } catch (SQLException e) {
            // Bỏ qua lỗi và thử cách khác
        }

        try {
            // Thử lấy dưới dạng timestamp (milliseconds từ epoch)
            long timestamp = rs.getLong(columnName);
            if (!rs.wasNull()) {
                return Instant.ofEpochMilli(timestamp)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();
            }
        } catch (SQLException e) {
            // Bỏ qua lỗi và thử cách khác
        }

        try {
            // Thử lấy dưới dạng chuỗi và parse
            String dateStr = rs.getString(columnName);
            if (dateStr != null && !dateStr.isEmpty()) {
                if (dateStr.matches("\\d+")) {
                    // Nếu chuỗi chỉ chứa số, xử lý như timestamp
                    long timestamp = Long.parseLong(dateStr);
                    return Instant.ofEpochMilli(timestamp)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate();
                } else {
                    // Thử parse theo định dạng ISO
                    return LocalDate.parse(dateStr);
                }
            }
        } catch (Exception e) {
            // Bỏ qua lỗi
        }

        // Trả về ngày hiện tại nếu không thể parse
        System.err.println("WARNING: Could not parse date from column " + columnName + ", using current date instead");
        return LocalDate.now();
    }

    public List<HealthEntry> findRecentEntries(int userId, int limit) throws SQLException {
        List<HealthEntry> entries = new ArrayList<>();
        Connection conn = DatabaseManager.getInstance().getConnection();
        String sql = "SELECT * FROM health_data WHERE user_id = ? ORDER BY record_date DESC LIMIT ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    // Sửa dòng này để sử dụng getLocalDateFromResultSet
                    LocalDate recordDate = getLocalDateFromResultSet(rs, "record_date");

                    HealthEntry entry = new HealthEntry(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            recordDate, // Sử dụng biến đã xử lý
                            rs.getObject("weight") != null ? rs.getDouble("weight") : null,
                            rs.getObject("systolic_bp") != null ? rs.getInt("systolic_bp") : null,
                            rs.getObject("diastolic_bp") != null ? rs.getInt("diastolic_bp") : null,
                            rs.getObject("sleep_minutes") != null ? rs.getDouble("sleep_minutes") / 60.0 : null,
                            rs.getObject("steps") != null ? rs.getInt("steps") : null,
                            rs.getObject("heart_rate") != null ? rs.getInt("heart_rate") : null,
                            rs.getObject("calories") != null ? rs.getInt("calories") : null,
                            rs.getObject("water_intake") != null ? rs.getDouble("water_intake") : null
                    );
                    entries.add(entry);
                }
            }
        }
        return entries;
    }

    public Optional<HealthEntry> findLatestEntryByUserId(int userId) throws SQLException {
        String sql = "SELECT * FROM health_data WHERE user_id = ? ORDER BY record_date DESC, id DESC LIMIT 1";
        try (PreparedStatement pstmt = DatabaseManager.getInstance().getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // Sử dụng phương thức getLocalDateFromResultSet
                    LocalDate recordDate = getLocalDateFromResultSet(rs, "record_date");

                    HealthEntry entry = new HealthEntry(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            recordDate, // Sử dụng biến đã xử lý
                            rs.getObject("weight") != null ? rs.getDouble("weight") : null,
                            rs.getObject("systolic_bp") != null ? rs.getInt("systolic_bp") : null,
                            rs.getObject("diastolic_bp") != null ? rs.getInt("diastolic_bp") : null,
                            // Đổi từ sleep_hours sang sleep_minutes và chuyển đổi đúng
                            rs.getObject("sleep_minutes") != null ? rs.getDouble("sleep_minutes") / 60.0 : null,
                            rs.getObject("steps") != null ? rs.getInt("steps") : null,
                            rs.getObject("heart_rate") != null ? rs.getInt("heart_rate") : null,
                            rs.getObject("calories") != null ? rs.getInt("calories") : null,
                            rs.getObject("water_intake") != null ? rs.getDouble("water_intake") : null
                    );
                    return Optional.of(entry);
                }
            }
        }
        return Optional.empty();
    }

    private HealthEntry mapResultSetToHealthEntry(ResultSet rs) throws SQLException {
        // Phương thức đúng để chuyển đổi timestamp về LocalDate
        LocalDate recordDate;
        try {
            // Trường hợp 1: Dữ liệu được lưu dưới dạng chuỗi ngày tháng
            recordDate = LocalDate.parse(rs.getString("record_date"));
        } catch (Exception e) {
            try {
                // Trường hợp 2: Dữ liệu được lưu dưới dạng timestamp
                long timestamp = rs.getLong("record_date");
                recordDate = Instant.ofEpochMilli(timestamp)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();
            } catch (Exception e2) {
                // Trường hợp 3: Dữ liệu được lưu dưới dạng date SQL
                recordDate = rs.getDate("record_date").toLocalDate();
            }
        }

        return new HealthEntry(
                rs.getInt("id"),
                rs.getInt("user_id"),
                recordDate,
                rs.getObject("weight") != null ? rs.getDouble("weight") : null,
                rs.getObject("systolic_bp") != null ? rs.getInt("systolic_bp") : null,
                rs.getObject("diastolic_bp") != null ? rs.getInt("diastolic_bp") : null,
                rs.getObject("sleep_minutes") != null ? rs.getDouble("sleep_minutes") / 60.0 : null,
                rs.getObject("steps") != null ? rs.getInt("steps") : null,
                rs.getObject("heart_rate") != null ? rs.getInt("heart_rate") : null,
                rs.getObject("calories") != null ? rs.getInt("calories") : null,
                rs.getObject("water_intake") != null ? rs.getDouble("water_intake") : null
        );
    }

}