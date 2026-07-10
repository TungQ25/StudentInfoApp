package com.example.taskmanager.dto;

import com.example.taskmanager.entity.HabitCompletion;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HabitCompletionRequest(
        String id,

        @NotBlank
        String habitId,

        @NotBlank
        String periodKey,

        long completedAt,
        long updatedAt,
        long version,
        String deviceId
) {
    public HabitCompletion toEntity() {
        HabitCompletion completion = new HabitCompletion();
        completion.setId(id);
        applyTo(completion);
        return completion;
    }

    public void applyTo(HabitCompletion completion) {
        completion.setHabitId(habitId);
        completion.setPeriodKey(periodKey);
        completion.setCompletedAt(completedAt);
        completion.setUpdatedAt(updatedAt);
    }
}
