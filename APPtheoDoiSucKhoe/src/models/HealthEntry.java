package models;

import java.time.LocalDate;

public class HealthEntry {

    private int entryId;
    private int userId;
    private LocalDate recordDate;
    private Double weight;
    private Double sleepHours;
    private Integer systolicBp;
    private Integer diastolicBp;

    private Integer steps;
    private Integer heartRate;
    private Integer calories;
    private Double waterIntake;

    public HealthEntry(int userId, LocalDate recordDate) {
        this.userId = userId;
        this.recordDate = recordDate;
    }

    public HealthEntry(int userId, LocalDate recordDate, Double weight, Integer systolicBp, Integer diastolicBp, Double sleepHours) {
        this.userId = userId;
        this.recordDate = recordDate;
        this.weight = weight;
        this.systolicBp = systolicBp;
        this.diastolicBp = diastolicBp;
        this.sleepHours = sleepHours;
    }

    public HealthEntry(int entryId, int userId, LocalDate recordDate, Double weight, Integer systolicBp, Integer diastolicBp, Double sleepHours) {
        this(userId, recordDate, weight, systolicBp, diastolicBp, sleepHours);
        this.entryId = entryId;
    }

    public HealthEntry(int entryId, int userId, LocalDate recordDate, Double weight, Integer systolicBp, Integer diastolicBp,
                       Double sleepHours, Integer steps, Integer heartRate, Integer calories, Double waterIntake) {
        this(entryId, userId, recordDate, weight, systolicBp, diastolicBp, sleepHours);
        this.steps = steps;
        this.heartRate = heartRate;
        this.calories = calories;
        this.waterIntake = waterIntake;
    }

    public LocalDate getEntryDate() {
        return this.recordDate;
    }

    public String getBloodPressure() {
        if (this.systolicBp != null && this.diastolicBp != null) {
            return this.systolicBp + "/" + this.diastolicBp;
        }
        return "N/A";
    }

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

    public Integer getSteps() { return steps; }
    public void setSteps(Integer steps) { this.steps = steps; }
    public Integer getHeartRate() { return heartRate; }
    public void setHeartRate(Integer heartRate) { this.heartRate = heartRate; }
    public Integer getCalories() { return calories; }
    public void setCalories(Integer calories) { this.calories = calories; }
    public Double getWaterIntake() { return waterIntake; }
    public void setWaterIntake(Double waterIntake) { this.waterIntake = waterIntake; }

    @Override
    public String toString() {
        return "HealthEntry{" +
                "userId=" + userId +
                ", date=" + recordDate +
                ", weight=" + weight +
                ", systolicBp=" + systolicBp +
                ", diastolicBp=" + diastolicBp +
                ", sleepHours=" + sleepHours +
                ", steps=" + steps +
                ", heartRate=" + heartRate +
                ", calories=" + calories +
                ", waterIntake=" + waterIntake +
                '}';
    }
}