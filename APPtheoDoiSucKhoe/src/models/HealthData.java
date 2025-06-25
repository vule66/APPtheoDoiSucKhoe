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

    public HealthData() {
        this.date = LocalDate.now();
        this.lastUpdated = LocalDateTime.now();
    }

    public HealthData(String userId) {
        this();
        this.userId = userId;
    }

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

    @Override
    public String toString() {
        return String.format("HealthData{userId='%s', date=%s, steps=%d, heartRate=%d, calories=%d, weight=%.1f}",
                userId, date, steps, heartRate, calories, weight);
    }
}