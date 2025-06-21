package services;

import database.DatabaseManager;
import models.Goal;
import models.User;
import models.HealthData;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service class để xử lý tất cả các thao tác liên quan đến Goals
 */
public class GoalService {

    private Connection connection;
    private HealthDataService healthDataService;

    public GoalService() {
        this.connection = DatabaseManager.getInstance().getConnection();
        this.healthDataService = new HealthDataService();
    }

    /**
     * Tạo mục tiêu mới
     */
    public boolean createGoal(Goal goal) {
        String sql = """
        INSERT INTO user_goals (user_id, goal_type, goal_description, target_value, target_unit, 
                              current_value, start_date, target_date, priority_level) 
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
    """;

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, goal.getUserId());
            stmt.setString(2, goal.getGoalType().getDisplayName());
            stmt.setString(3, goal.getGoalDescription());
            stmt.setDouble(4, goal.getTargetValue() != null ? goal.getTargetValue() : 0);
            stmt.setString(5, goal.getTargetUnit());
            stmt.setDouble(6, goal.getCurrentValue() != null ? goal.getCurrentValue() : 0);
            stmt.setString(7, goal.getStartDate().toString());
            stmt.setString(8, goal.getTargetDate().toString());
            stmt.setString(9, goal.getPriority().getDisplayName());

            System.out.println("INSERT goal: userId=" + goal.getUserId() +
                    ", type=" + goal.getGoalType().getDisplayName() +
                    ", desc=" + goal.getGoalDescription() +
                    ", targetVal=" + goal.getTargetValue() +
                    ", unit=" + goal.getTargetUnit() +
                    ", currVal=" + goal.getCurrentValue() +
                    ", start=" + goal.getStartDate() +
                    ", target=" + goal.getTargetDate() +
                    ", priority=" + goal.getPriority().getDisplayName());

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    goal.setId(generatedKeys.getInt(1));
                }
                System.out.println("✅ Goal created successfully: " + goal.getGoalDescription());
                return true;
            }

        } catch (SQLException e) {
            System.err.println("❌ Error creating goal: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // Tương tự sửa ở updateGoal:
    public boolean updateGoal(Goal goal) {
        String sql = """
        UPDATE user_goals 
        SET goal_type = ?, goal_description = ?, target_value = ?, target_unit = ?, 
            current_value = ?, start_date = ?, target_date = ?, is_achieved = ?, 
            priority_level = ?, achieved_at = ?
        WHERE id = ?
    """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, goal.getGoalType().getDisplayName());
            stmt.setString(2, goal.getGoalDescription());
            stmt.setDouble(3, goal.getTargetValue() != null ? goal.getTargetValue() : 0);
            stmt.setString(4, goal.getTargetUnit());
            stmt.setDouble(5, goal.getCurrentValue() != null ? goal.getCurrentValue() : 0);
            stmt.setString(6, goal.getStartDate().toString());
            stmt.setString(7, goal.getTargetDate().toString());
            stmt.setBoolean(8, goal.isAchieved());
            stmt.setString(9, goal.getPriority().getDisplayName());

            if (goal.getAchievedAt() != null) {
                stmt.setString(10, goal.getAchievedAt().toString());
            } else {
                stmt.setNull(10, Types.VARCHAR);
            }

            stmt.setInt(11, goal.getId());

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Goal updated successfully: " + goal.getGoalDescription());
                return true;
            }

        } catch (SQLException e) {
            System.err.println("❌ Error updating goal: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    public List<Goal> getUserGoals(User user) {
        return getUserGoals(user, false);
    }

    public List<Goal> getUserGoals(User user, boolean includeCompleted) {
        List<Goal> goals = new ArrayList<>();

        String sql = """
            SELECT id, user_id, goal_type, goal_description, target_value, target_unit, 
                   current_value, start_date, target_date, is_achieved, priority_level, 
                   created_at, achieved_at
            FROM user_goals 
            WHERE user_id = ?
        """;

        if (!includeCompleted) {
            sql += " AND is_achieved = FALSE";
        }

        sql += " ORDER BY priority_level DESC, created_at DESC";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, user.getId());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Goal goal = mapResultSetToGoal(rs);
                goals.add(goal);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error fetching user goals: " + e.getMessage());
            e.printStackTrace();
        }

        return goals;
    }

    public Goal getGoalById(int goalId) {
        String sql = """
            SELECT id, user_id, goal_type, goal_description, target_value, target_unit, 
                   current_value, start_date, target_date, is_achieved, priority_level, 
                   created_at, achieved_at
            FROM user_goals 
            WHERE id = ?
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, goalId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToGoal(rs);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error fetching goal by ID: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    public boolean deleteGoal(int goalId) {
        String sql = "DELETE FROM user_goals WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, goalId);

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Goal deleted successfully");
                return true;
            }

        } catch (SQLException e) {
            System.err.println("❌ Error deleting goal: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }


    public void updateGoalProgress(User user) {
        List<Goal> activeGoals = getUserGoals(user, false);
        HealthData todayData = healthDataService.getTodayData(user);

        for (Goal goal : activeGoals) {
            Double newValue = calculateCurrentProgress(goal, todayData, user);

            if (newValue != null && !newValue.equals(goal.getCurrentValue())) {
                goal.updateProgress(newValue);
                updateGoal(goal);

                // Kiểm tra xem có đạt mục tiêu mới không
                if (goal.isAchieved() && goal.getAchievedAt() != null) {
                    System.out.println("🎉 Goal achieved: " + goal.getGoalDescription());
                    // Có thể thêm notification hoặc achievement system ở đây
                }
            }
        }
    }


    private Double calculateCurrentProgress(Goal goal, HealthData todayData, User user) {
        switch (goal.getGoalType()) {
            case STEPS:
                return (double) todayData.getSteps();

            case WATER_INTAKE:
                return todayData.getWaterIntake();

            case CALORIES_BURN:
                return (double) todayData.getCalories();

            case SLEEP_HOURS:
                return todayData.getSleepMinutes() / 60.0; // Convert minutes to hours

            case WEIGHT_LOSS:
            case WEIGHT_GAIN:
                return todayData.getWeight();

            case HEART_RATE:
                return (double) todayData.getHeartRate();

            case EXERCISE_DURATION:
                return calculateDailyExerciseDuration(user);

            case BLOOD_PRESSURE:
                // Có thể return systolic hoặc diastolic, tùy theo target
                return (double) todayData.getSystolicBP();

            default:
                return goal.getCurrentValue(); // Keep current value for CUSTOM goals
        }
    }


    private Double calculateDailyExerciseDuration(User user) {
        String sql = """
            SELECT SUM(duration_minutes) as total_duration
            FROM exercises 
            WHERE user_id = ? AND exercise_date = ?
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, user.getId());
            stmt.setString(2, LocalDate.now().toString());

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return (double) rs.getInt("total_duration");
            }

        } catch (SQLException e) {
            System.err.println("❌ Error calculating exercise duration: " + e.getMessage());
        }

        return 0.0;
    }

    public GoalStatistics getGoalStatistics(User user) {
        String sql = """
            SELECT 
                COUNT(*) as total_goals,
                SUM(CASE WHEN is_achieved = 1 THEN 1 ELSE 0 END) as completed_goals,
                SUM(CASE WHEN is_achieved = 0 AND target_date < date('now') THEN 1 ELSE 0 END) as overdue_goals,
                SUM(CASE WHEN is_achieved = 0 AND target_date >= date('now') THEN 1 ELSE 0 END) as active_goals
            FROM user_goals 
            WHERE user_id = ?
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, user.getId());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new GoalStatistics(
                        rs.getInt("total_goals"),
                        rs.getInt("completed_goals"),
                        rs.getInt("overdue_goals"),
                        rs.getInt("active_goals")
                );
            }

        } catch (SQLException e) {
            System.err.println("❌ Error fetching goal statistics: " + e.getMessage());
        }

        return new GoalStatistics(0, 0, 0, 0);
    }

    /**
     * Map ResultSet to Goal object
     */
    private Goal mapResultSetToGoal(ResultSet rs) throws SQLException {
        LocalDateTime createdAt = null;
        LocalDateTime achievedAt = null;

        String createdAtStr = rs.getString("created_at");
        if (createdAtStr != null) {
            createdAt = LocalDateTime.parse(createdAtStr.replace(" ", "T"));
        }

        String achievedAtStr = rs.getString("achieved_at");
        if (achievedAtStr != null) {
            achievedAt = LocalDateTime.parse(achievedAtStr.replace(" ", "T"));
        }

        return new Goal(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getString("goal_type"),
                rs.getString("goal_description"),
                rs.getDouble("target_value"),
                rs.getString("target_unit"),
                rs.getDouble("current_value"),
                LocalDate.parse(rs.getString("start_date")),
                LocalDate.parse(rs.getString("target_date")),
                rs.getBoolean("is_achieved"),
                rs.getString("priority_level"),
                createdAt,
                achievedAt
        );
    }

    /**
     * Inner class for goal statistics
     */
    public static class GoalStatistics {
        private final int totalGoals;
        private final int completedGoals;
        private final int overdueGoals;
        private final int activeGoals;

        public GoalStatistics(int totalGoals, int completedGoals, int overdueGoals, int activeGoals) {
            this.totalGoals = totalGoals;
            this.completedGoals = completedGoals;
            this.overdueGoals = overdueGoals;
            this.activeGoals = activeGoals;
        }

        public int getTotalGoals() { return totalGoals; }
        public int getCompletedGoals() { return completedGoals; }
        public int getOverdueGoals() { return overdueGoals; }
        public int getActiveGoals() { return activeGoals; }

        public double getCompletionRate() {
            return totalGoals > 0 ? (double) completedGoals / totalGoals * 100 : 0;
        }

        @Override
        public String toString() {
            return String.format("GoalStats{total=%d, completed=%d, overdue=%d, active=%d, rate=%.1f%%}",
                    totalGoals, completedGoals, overdueGoals, activeGoals, getCompletionRate());
        }
    }
}