package models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class Goal {

    public enum GoalType {
        WEIGHT_LOSS("Weight Loss", "kg"),
        WEIGHT_GAIN("Weight Gain", "kg"),
        STEPS("Steps", "steps/day"),
        WATER_INTAKE("Water Intake", "L/day"),
        CALORIES_BURN("Calories Burn", "calories/day"),
        EXERCISE_DURATION("Exercise Duration", "minutes/day"),
        SLEEP_HOURS("Sleep Hours", "hours/day"),
        BLOOD_PRESSURE("Blood Pressure", "mmHg"),
        HEART_RATE("Heart Rate", "BPM"),
        CUSTOM("Custom", "");

        private final String displayName;
        private final String defaultUnit;

        GoalType(String displayName, String defaultUnit) {
            this.displayName = displayName;
            this.defaultUnit = defaultUnit;
        }

        public String getDisplayName() { return displayName; }
        public String getDefaultUnit() { return defaultUnit; }
    }

    // Enum cho mức độ ưu tiên
    public enum Priority {
        LOW("Low"), MEDIUM("Medium"), HIGH("High");

        private final String displayName;

        Priority(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() { return displayName; }
    }

    // Thuộc tính chính
    private int id;
    private int userId;
    private GoalType goalType;
    private String goalDescription;
    private Double targetValue;
    private String targetUnit;
    private Double currentValue;
    private LocalDate startDate;
    private LocalDate targetDate;
    private boolean isAchieved;
    private Priority priority;
    private LocalDateTime createdAt;
    private LocalDateTime achievedAt;

    // Constructors
    public Goal() {
        this.startDate = LocalDate.now();
        this.currentValue = 0.0;
        this.isAchieved = false;
        this.priority = Priority.MEDIUM;
        this.createdAt = LocalDateTime.now();
    }

    public Goal(int userId, GoalType goalType, String description, Double targetValue) {
        this();
        this.userId = userId;
        this.goalType = goalType;
        this.goalDescription = description;
        this.targetValue = targetValue;
        this.targetUnit = goalType.getDefaultUnit();
    }

    // Constructor đầy đủ cho việc load từ database
    public Goal(int id, int userId, String goalTypeStr, String goalDescription,
                Double targetValue, String targetUnit, Double currentValue,
                LocalDate startDate, LocalDate targetDate, boolean isAchieved,
                String priorityStr, LocalDateTime createdAt, LocalDateTime achievedAt) {
        this.id = id;
        this.userId = userId;
        this.goalType = parseGoalType(goalTypeStr);
        this.goalDescription = goalDescription;
        this.targetValue = targetValue;
        this.targetUnit = targetUnit;
        this.currentValue = currentValue != null ? currentValue : 0.0;
        this.startDate = startDate;
        this.targetDate = targetDate;
        this.isAchieved = isAchieved;
        this.priority = parsePriority(priorityStr);
        this.createdAt = createdAt;
        this.achievedAt = achievedAt;
    }

    // Utility methods
    private GoalType parseGoalType(String goalTypeStr) {
        try {
            return GoalType.valueOf(goalTypeStr.toUpperCase().replace(" ", "_"));
        } catch (Exception e) {
            return GoalType.CUSTOM;
        }
    }

    private Priority parsePriority(String priorityStr) {
        try {
            return Priority.valueOf(priorityStr.toUpperCase());
        } catch (Exception e) {
            return Priority.MEDIUM;
        }
    }

    public double getProgressPercentage() {
        if (targetValue == null || targetValue == 0) return 0.0;

        double progress = currentValue / targetValue;

        // Đối với mục tiêu giảm cân, logic khác biệt
        if (goalType == GoalType.WEIGHT_LOSS) {
            // Giả sử targetValue là cân nặng mục tiêu, currentValue là cân nặng hiện tại
            // Progress = (initial_weight - current_weight) / (initial_weight - target_weight)
            // Cần thêm initial_weight vào model hoặc tính toán khác
            return Math.min(progress, 1.0);
        }

        return Math.min(progress, 1.0);
    }

    public long getDaysRemaining() {
        if (targetDate == null) return 0;
        return ChronoUnit.DAYS.between(LocalDate.now(), targetDate);
    }


    public long getTotalDays() {
        if (startDate == null || targetDate == null) return 0;
        return ChronoUnit.DAYS.between(startDate, targetDate);
    }

    public boolean isOverdue() {
        return !isAchieved && targetDate != null && LocalDate.now().isAfter(targetDate);
    }

    public String getStatus() {
        if (isAchieved) return "Completed";
        if (isOverdue()) return "Overdue";
        if (getDaysRemaining() <= 7) return "Due Soon";
        return "In Progress";
    }

    public void updateProgress(Double newValue) {
        this.currentValue = newValue;

        // Kiểm tra đã đạt mục tiêu chưa
        if (!isAchieved && targetValue != null) {
            boolean achieved = false;

            switch (goalType) {
                case WEIGHT_LOSS:
                    achieved = currentValue <= targetValue;
                    break;
                case WEIGHT_GAIN:
                    achieved = currentValue >= targetValue;
                    break;
                default:
                    achieved = currentValue >= targetValue;
                    break;
            }

            if (achieved) {
                this.isAchieved = true;
                this.achievedAt = LocalDateTime.now();
            }
        }
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public GoalType getGoalType() { return goalType; }
    public void setGoalType(GoalType goalType) {
        this.goalType = goalType;
        if (this.targetUnit == null || this.targetUnit.isEmpty()) {
            this.targetUnit = goalType.getDefaultUnit();
        }
    }

    public String getGoalDescription() { return goalDescription; }
    public void setGoalDescription(String goalDescription) { this.goalDescription = goalDescription; }

    public Double getTargetValue() { return targetValue; }
    public void setTargetValue(Double targetValue) { this.targetValue = targetValue; }

    public String getTargetUnit() { return targetUnit; }
    public void setTargetUnit(String targetUnit) { this.targetUnit = targetUnit; }

    public Double getCurrentValue() { return currentValue; }
    public void setCurrentValue(Double currentValue) { this.currentValue = currentValue; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }

    public boolean isAchieved() { return isAchieved; }
    public void setAchieved(boolean achieved) {
        this.isAchieved = achieved;
        if (achieved && achievedAt == null) {
            this.achievedAt = LocalDateTime.now();
        }
    }

    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getAchievedAt() { return achievedAt; }
    public void setAchievedAt(LocalDateTime achievedAt) { this.achievedAt = achievedAt; }

    @Override
    public String toString() {
        return String.format("Goal{id=%d, type=%s, description='%s', progress=%.1f%%, status=%s}",
                id, goalType.getDisplayName(), goalDescription,
                getProgressPercentage() * 100, getStatus());
    }
}