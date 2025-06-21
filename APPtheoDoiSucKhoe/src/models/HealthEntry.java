package models;

import java.time.LocalDate;

/**
 * Model này đại diện cho một dòng trong bảng 'health_data' của bạn.
 * Các trường đã được cập nhật để khớp chính xác với database.
 */
public class HealthEntry {

    private int entryId;
    private int userId;
    private LocalDate recordDate;
    private Double weight;
    private Double sleepHours;
    private Integer systolicBp;
    private Integer diastolicBp;
    // Bạn có thể thêm các trường khác từ bảng health_data nếu cần (steps, mood_rating, etc.)

    // Constructor để tạo một entry mới từ giao diện
    public HealthEntry(int userId, LocalDate recordDate, Double weight, Integer systolicBp, Integer diastolicBp, Double sleepHours) {
        this.userId = userId;
        this.recordDate = recordDate;
        this.weight = weight;
        this.systolicBp = systolicBp;
        this.diastolicBp = diastolicBp;
        this.sleepHours = sleepHours;
    }

    // Constructor để đọc một entry từ database
    public HealthEntry(int entryId, int userId, LocalDate recordDate, Double weight, Integer systolicBp, Integer diastolicBp, Double sleepHours) {
        this(userId, recordDate, weight, systolicBp, diastolicBp, sleepHours);
        this.entryId = entryId;
    }

    // Getters and Setters
    public int getEntryId() { return entryId; }
    public void setEntryId(int entryId) { this.entryId = entryId; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public LocalDate getRecordDate() { return recordDate; }
    public void setRecordDate(LocalDate recordDate) { this.recordDate = recordDate; }
    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }
    public Double getSleepHours() { return sleepHours; }
    public void setSleepHours(Double sleepHours) { this.sleepHours = sleepHours; }
    public Integer getSystolicBp() { return systolicBp; }
    public void setSystolicBp(Integer systolicBp) { this.systolicBp = systolicBp; }
    public Integer getDiastolicBp() { return diastolicBp; }
    public void setDiastolicBp(Integer diastolicBp) { this.diastolicBp = diastolicBp; }
}