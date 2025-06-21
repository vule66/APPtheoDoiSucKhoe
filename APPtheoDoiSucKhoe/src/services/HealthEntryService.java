package services;

import database.DatabaseManager;
import models.HealthEntry;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service này đã được viết lại để tương tác chính xác với bảng 'health_data'.
 */
public class HealthEntryService {

    private Connection getConnection() throws SQLException {
        return DatabaseManager.getInstance().getConnection();
    }

    public void saveOrUpdateEntry(HealthEntry entry) throws SQLException {
        Optional<HealthEntry> existingEntry = findEntryByUserIdAndDate(entry.getUserId(), entry.getRecordDate());

        String sql;
        if (existingEntry.isPresent()) {
            // Dùng đúng tên cột: weight, systolic_bp, diastolic_bp, sleep_hours
            sql = "UPDATE health_data SET weight = ?, systolic_bp = ?, diastolic_bp = ?, sleep_hours = ? WHERE id = ?";
        } else {
            // Dùng đúng tên bảng và cột: health_data, user_id, record_date...
            sql = "INSERT INTO health_data(user_id, record_date, weight, systolic_bp, diastolic_bp, sleep_hours) VALUES(?, ?, ?, ?, ?, ?)";
        }

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            if (existingEntry.isPresent()) {
                // Tham số cho lệnh UPDATE
                pstmt.setObject(1, entry.getWeight());
                pstmt.setObject(2, entry.getSystolicBp());
                pstmt.setObject(3, entry.getDiastolicBp());
                pstmt.setObject(4, entry.getSleepHours());
                pstmt.setInt(5, existingEntry.get().getEntryId());
            } else {
                // Tham số cho lệnh INSERT
                pstmt.setInt(1, entry.getUserId());
                pstmt.setString(2, entry.getRecordDate().toString());
                pstmt.setObject(3, entry.getWeight());
                pstmt.setObject(4, entry.getSystolicBp());
                pstmt.setObject(5, entry.getDiastolicBp());
                pstmt.setObject(6, entry.getSleepHours());
            }
            pstmt.executeUpdate();
        }
    }

    public Optional<HealthEntry> findEntryByUserIdAndDate(int userId, LocalDate date) throws SQLException {
        // Dùng đúng tên bảng và cột
        String sql = "SELECT * FROM health_data WHERE user_id = ? AND record_date = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, date.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToHealthEntry(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<HealthEntry> findRecentEntries(int userId, int limit) throws SQLException {
        List<HealthEntry> entries = new ArrayList<>();
        // Dùng đúng tên bảng và cột, sắp xếp theo ngày giảm dần
        String sql = "SELECT * FROM health_data WHERE user_id = ? ORDER BY record_date DESC, id DESC LIMIT ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    entries.add(mapResultSetToHealthEntry(rs));
                }
            }
        }
        return entries;
    }

    private HealthEntry mapResultSetToHealthEntry(ResultSet rs) throws SQLException {
        return new HealthEntry(
                rs.getInt("id"),
                rs.getInt("user_id"),
                LocalDate.parse(rs.getString("record_date")),
                rs.getObject("weight") != null ? rs.getDouble("weight") : null,
                rs.getObject("systolic_bp") != null ? rs.getInt("systolic_bp") : null,
                rs.getObject("diastolic_bp") != null ? rs.getInt("diastolic_bp") : null,
                rs.getObject("sleep_hours") != null ? rs.getDouble("sleep_hours") : null
        );
    }
}