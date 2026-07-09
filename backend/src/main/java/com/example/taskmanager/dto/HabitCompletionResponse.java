package com.example.taskmanager.dto;

import com.example.taskmanager.entity.HabitCompletion;

public record HabitCompletionResponse(
        String id,
        String habitId,
        String userId,
        String periodKey,
        long completedAt,
        long updatedAt,
        boolean deleted,
        Long deletedAt,
        long version,
        String lastModifiedDeviceId
) {
    public static HabitCompletionResponse from(HabitCompletion completion) {
        return new HabitCompletionResponse(
                completion.getId(),
                completion.getHabitId(),
                completion.getUserId(),
                completion.getPeriodKey(),
                completion.getCompletedAt(),
                completion.getUpdatedAt(),
                completion.isDeleted(),
                completion.getDeletedAt(),
                completion.getVersion(),
                completion.getLastModifiedDeviceId()
        );
    }
}
