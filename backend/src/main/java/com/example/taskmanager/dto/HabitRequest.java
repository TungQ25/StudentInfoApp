package com.example.taskmanager.dto;

import com.example.taskmanager.entity.Habit;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HabitRequest(
        String id,

        @NotBlank
        String title,

        String groupName,
        String frequency,
        int totalDays,
        int color,
        int sortOrder,
        long createdAt,
        long updatedAt,
        long version,
        String deviceId
) {
    public Habit toEntity() {
        Habit habit = new Habit();
        habit.setId(id);
        habit.setCreatedAt(createdAt);
        applyTo(habit);
        return habit;
    }

    public void applyTo(Habit habit) {
        habit.setTitle(title);
        habit.setGroupName(groupName);
        habit.setFrequency(frequency);
        habit.setTotalDays(totalDays);
        habit.setColor(color);
        habit.setSortOrder(sortOrder);
        habit.setUpdatedAt(updatedAt);
    }
}
