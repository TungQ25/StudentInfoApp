package com.example.taskmanager.dto;

import com.example.taskmanager.entity.Habit;

public record HabitResponse(
        String id,
        String userId,
        String title,
        String groupName,
        String frequency,
        int totalDays,
        int color,
        int sortOrder,
        long createdAt,
        long updatedAt,
        boolean deleted,
        Long deletedAt,
        long version,
        String lastModifiedDeviceId
) {
    public static HabitResponse from(Habit habit) {
        return new HabitResponse(
                habit.getId(),
                habit.getUserId(),
                habit.getTitle(),
                habit.getGroupName(),
                habit.getFrequency(),
                habit.getTotalDays(),
                habit.getColor(),
                habit.getSortOrder(),
                habit.getCreatedAt(),
                habit.getUpdatedAt(),
                habit.isDeleted(),
                habit.getDeletedAt(),
                habit.getVersion(),
                habit.getLastModifiedDeviceId()
        );
    }
}
