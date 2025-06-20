package models;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class HealthData {
    private String userId;
    private LocalDate date;
    private int steps;
    private int heartRate;
    private int calories;
    private int sleepMinutes;
    private double waterIntake;
    private double weight;
    private int systolicBP;
    private int diastolicBP;
    private LocalDateTime lastUpdated;

    // Constructors
    public HealthData() {
        this.date = LocalDate.now();
        this.lastUpdated = LocalDateTime.now();
    }

    public HealthData(String userId) {
        this();
        this.userId = userId;
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public int getSteps() {
        return steps;
    }

    public void setSteps(int steps) {
        this.steps = steps;
        this.lastUpdated = LocalDateTime.now();
    }

    public int getHeartRate() {
        return heartRate;
    }

    public void setHeartRate(int heartRate) {
        this.heartRate = heartRate;
        this.lastUpdated = LocalDateTime.now();
    }

    public int getCalories() {
        return calories;
    }

    public void setCalories(int calories) {
        this.calories = calories;
        this.lastUpdated = LocalDateTime.now();
    }

    public int getSleepMinutes() {
        return sleepMinutes;
    }

    public void setSleepMinutes(int sleepMinutes) {
        this.sleepMinutes = sleepMinutes;
        this.lastUpdated = LocalDateTime.now();
    }

    public double getWaterIntake() {
        return waterIntake;
    }

    public void setWaterIntake(double waterIntake) {
        this.waterIntake = waterIntake;
        this.lastUpdated = LocalDateTime.now();
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
        this.lastUpdated = LocalDateTime.now();
    }

    public int getSystolicBP() {
        return systolicBP;
    }

    public void setSystolicBP(int systolicBP) {
        this.systolicBP = systolicBP;
        this.lastUpdated = LocalDateTime.now();
    }

    public int getDiastolicBP() {
        return diastolicBP;
    }

    public void setDiastolicBP(int diastolicBP) {
        this.diastolicBP = diastolicBP;
        this.lastUpdated = LocalDateTime.now();
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    // Utility methods
    public double getStepsProgress() {
        return Math.min(steps / 10000.0, 1.0);
    }

    public double getCaloriesProgress() {
        return Math.min(calories / 2500.0, 1.0);
    }

    public double getWaterProgress() {
        return Math.min(waterIntake / 2.5, 1.0);
    }

    public String getBloodPressureString() {
        if (systolicBP > 0 && diastolicBP > 0) {
            return systolicBP + "/" + diastolicBP;
        }
        return "N/A";
    }

    public String getBloodPressureStatus() {
        if (systolicBP <= 0 || diastolicBP <= 0) return "Unknown";

        if (systolicBP < 120 && diastolicBP < 80) return "Normal";
        if (systolicBP < 130 && diastolicBP < 80) return "Elevated";
        if (systolicBP < 140 || diastolicBP < 90) return "High Stage 1";
        if (systolicBP < 180 || diastolicBP < 120) return "High Stage 2";
        return "Crisis";
    }

    @Override
    public String toString() {
        return String.format("HealthData{userId='%s', date=%s, steps=%d, heartRate=%d, calories=%d, weight=%.1f}",
                userId, date, steps, heartRate, calories, weight);
    }
}