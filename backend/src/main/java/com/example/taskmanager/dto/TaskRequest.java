package com.example.taskmanager.dto;

import com.example.taskmanager.entity.Task;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TaskRequest(
        String id,

        @NotBlank
        String title,

        String description,
        String categoryId,
        String deadline,
        boolean completed,
        boolean wontDo,
        String priority,
        String imagePath,
        long updatedAt,
        long version,
        String deviceId
) {
    public Task toEntity() {
        Task task = new Task();
        task.setId(id);
        applyTo(task);
        return task;
    }

    public void applyTo(Task task) {
        task.setTitle(title);
        task.setDescription(description);
        task.setCategoryId(categoryId);
        task.setDeadline(deadline);
        task.setCompleted(completed);
        task.setWontDo(wontDo);
        task.setPriority(priority);
        task.setImagePath(imagePath);
        task.setUpdatedAt(updatedAt);
    }
}
