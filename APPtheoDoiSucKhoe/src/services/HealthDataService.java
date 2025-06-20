package services;

import database.DatabaseManager;
import models.HealthData;
import models.User;
import java.sql.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class HealthDataService {

    private Connection connection;
    private Random random;

    public HealthDataService() {
        connection = DatabaseManager.getInstance().getConnection();
        random = new Random();
    }

    public HealthData getTodayData(User user) {
        if (user == null) return new HealthData();

        try {
            String sql = "SELECT * FROM health_data WHERE user_id = ? AND date = ?";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, user.getId());
            stmt.setString(2, LocalDate.now().toString());

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                HealthData data = new HealthData();
                data.setUserId(String.valueOf(user.getId()));
                data.setDate(LocalDate.parse(rs.getString("date")));
                data.setSteps(rs.getInt("steps"));
                data.setHeartRate(rs.getInt("heart_rate"));
                data.setCalories(rs.getInt("calories"));
                data.setSleepMinutes(rs.getInt("sleep_minutes"));
                data.setWaterIntake(rs.getDouble("water_intake"));
                data.setWeight(rs.getDouble("weight"));
                data.setSystolicBP(rs.getInt("systolic_bp"));
                data.setDiastolicBP(rs.getInt("diastolic_bp"));

                rs.close();
                stmt.close();
                return data;
            }

            rs.close();
            stmt.close();

            // If no data for today, create new entry with default values
            return createTodayData(user);

        } catch (SQLException e) {
            e.printStackTrace();
            return createDefaultData(user);
        }
    }

    private HealthData createTodayData(User user) {
        HealthData data = new HealthData();
        data.setUserId(String.valueOf(user.getId()));
        data.setDate(LocalDate.now());

        // Initialize with sample data
        data.setSteps(random.nextInt(5000) + 3000); // 3000-8000 steps
        data.setHeartRate(random.nextInt(20) + 65); // 65-85 BPM
        data.setCalories(random.nextInt(1000) + 1200); // 1200-2200 calories
        data.setSleepMinutes(random.nextInt(120) + 360); // 6-8 hours
        data.setWaterIntake(random.nextDouble() * 1.5 + 1.0); // 1.0-2.5L
        data.setWeight(user.getHeight() > 0 ? (user.getHeight() - 100) + random.nextDouble() * 10 : 70.0);
        data.setSystolicBP(random.nextInt(30) + 110); // 110-140
        data.setDiastolicBP(random.nextInt(20) + 70); // 70-90

        // Save to database
        saveTodayData(user, data);

        return data;
    }

    private HealthData createDefaultData(User user) {
        HealthData data = new HealthData();
        data.setUserId(user != null ? String.valueOf(user.getId()) : "0");
        data.setSteps(8542);
        data.setHeartRate(72);
        data.setCalories(1845);
        data.setSleepMinutes(443); // 7h 23m
        data.setWaterIntake(1.8);
        data.setWeight(68.5);
        data.setSystolicBP(120);
        data.setDiastolicBP(80);
        return data;
    }

    private void saveTodayData(User user, HealthData data) {
        try {
            String sql = """
                INSERT OR REPLACE INTO health_data 
                (user_id, date, steps, heart_rate, calories, sleep_minutes, water_intake, weight, systolic_bp, diastolic_bp)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, user.getId());
            stmt.setString(2, data.getDate().toString());
            stmt.setInt(3, data.getSteps());
            stmt.setInt(4, data.getHeartRate());
            stmt.setInt(5, data.getCalories());
            stmt.setInt(6, data.getSleepMinutes());
            stmt.setDouble(7, data.getWaterIntake());
            stmt.setDouble(8, data.getWeight());
            stmt.setInt(9, data.getSystolicBP());
            stmt.setInt(10, data.getDiastolicBP());

            stmt.executeUpdate();
            stmt.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveWeight(User user, double weight) {
        if (user == null) return;

        try {
            String sql = """
                INSERT OR REPLACE INTO health_data 
                (user_id, date, weight, updated_at)
                VALUES (?, ?, ?, CURRENT_TIMESTAMP)
                ON CONFLICT(user_id, date) DO UPDATE SET 
                weight = excluded.weight, updated_at = CURRENT_TIMESTAMP
            """;

            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, user.getId());
            stmt.setString(2, LocalDate.now().toString());
            stmt.setDouble(3, weight);

            stmt.executeUpdate();
            stmt.close();

            System.out.println("Saved weight: " + weight + " kg for user: " + user.getUsername());

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveWaterIntake(User user, double liters) {
        if (user == null) return;

        try {
            // Get current water intake and add to it
            HealthData currentData = getTodayData(user);
            double newTotal = currentData.getWaterIntake() + liters;

            String sql = """
                INSERT OR REPLACE INTO health_data 
                (user_id, date, water_intake, updated_at)
                VALUES (?, ?, ?, CURRENT_TIMESTAMP)
                ON CONFLICT(user_id, date) DO UPDATE SET 
                water_intake = excluded.water_intake, updated_at = CURRENT_TIMESTAMP
            """;

            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, user.getId());
            stmt.setString(2, LocalDate.now().toString());
            stmt.setDouble(3, newTotal);

            stmt.executeUpdate();
            stmt.close();

            System.out.println("Added water intake: " + liters + "L (total: " + newTotal + "L) for user: " + user.getUsername());

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveBloodPressure(User user, int systolic, int diastolic) {
        if (user == null) return;

        try {
            String sql = """
                INSERT OR REPLACE INTO health_data 
                (user_id, date, systolic_bp, diastolic_bp, updated_at)
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
                ON CONFLICT(user_id, date) DO UPDATE SET 
                systolic_bp = excluded.systolic_bp, 
                diastolic_bp = excluded.diastolic_bp, 
                updated_at = CURRENT_TIMESTAMP
            """;

            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setInt(1, user.getId());
            stmt.setString(2, LocalDate.now().toString());
            stmt.setInt(3, systolic);
            stmt.setInt(4, diastolic);

            stmt.executeUpdate();
            stmt.close();

            System.out.println("Saved blood pressure: " + systolic + "/" + diastolic + " for user: " + user.getUsername());

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveExercise(User user, String type, int durationMinutes, int calories) {
        if (user == null) return;

        try {
            // Save exercise record
            String exerciseSql = """
                INSERT INTO exercises (user_id, exercise_type, duration_minutes, calories_burned, exercise_date)
                VALUES (?, ?, ?, ?, ?)
            """;

            PreparedStatement exerciseStmt = connection.prepareStatement(exerciseSql);
            exerciseStmt.setInt(1, user.getId());
            exerciseStmt.setString(2, type);
            exerciseStmt.setInt(3, durationMinutes);
            exerciseStmt.setInt(4, calories);
            exerciseStmt.setString(5, LocalDate.now().toString());

            exerciseStmt.executeUpdate();
            exerciseStmt.close();

            // Update daily calories and steps
            HealthData currentData = getTodayData(user);
            int newCalories = currentData.getCalories() + calories;
            int estimatedSteps = estimateStepsFromExercise(type, durationMinutes);
            int newSteps = currentData.getSteps() + estimatedSteps;

            String updateSql = """
                INSERT OR REPLACE INTO health_data 
                (user_id, date, calories, steps, updated_at)
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
                ON CONFLICT(user_id, date) DO UPDATE SET 
                calories = excluded.calories, 
                steps = excluded.steps,
                updated_at = CURRENT_TIMESTAMP
            """;

            PreparedStatement updateStmt = connection.prepareStatement(updateSql);
            updateStmt.setInt(1, user.getId());
            updateStmt.setString(2, LocalDate.now().toString());
            updateStmt.setInt(3, newCalories);
            updateStmt.setInt(4, newSteps);

            updateStmt.executeUpdate();
            updateStmt.close();

            System.out.println("Saved exercise: " + type + " (" + durationMinutes + " min, " + calories + " cal) for user: " + user.getUsername());

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private int estimateStepsFromExercise(String exerciseType, int minutes) {
        switch (exerciseType.toLowerCase()) {
            case "running":
                return minutes * 150;
            case "walking":
                return minutes * 100;
            case "cycling":
                return minutes * 50;
            case "swimming":
                return minutes * 30;
            default:
                return minutes * 75;
        }
    }
}