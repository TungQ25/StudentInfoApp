package com.example.taskmanager.dto;

import com.example.taskmanager.entity.Task;

public record TaskResponse(
        String id,
        String title,
        String description,
        String categoryId,
        String deadline,
        boolean completed,
        boolean wontDo,
        String priority,
        String imagePath,
        long updatedAt,
        boolean deleted,
        Long deletedAt,
        String userId,
        long version,
        String lastModifiedDeviceId
) {
    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getCategoryId(),
                task.getDeadline(),
                task.isCompleted(),
                task.isWontDo(),
                task.getPriority(),
                task.getImagePath(),
                task.getUpdatedAt(),
                task.isDeleted(),
                task.getDeletedAt(),
                task.getUserId(),
                task.getVersion(),
                task.getLastModifiedDeviceId()
        );
    }
}
