package com.example.taskmanager.dto;

import com.example.taskmanager.entity.Category;

public record CategoryResponse(
        String id,
        String userId,
        String name,
        String icon,
        String color,
        boolean pinned,
        int pinnedOrder,
        int sortOrder,
        boolean hidden,
        long createdAt,
        long updatedAt,
        boolean deleted,
        Long deletedAt,
        long version,
        String lastModifiedDeviceId
) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getUserId(),
                category.getName(),
                category.getIcon(),
                category.getColor(),
                category.isPinned(),
                category.getPinnedOrder(),
                category.getSortOrder(),
                category.isHidden(),
                category.getCreatedAt(),
                category.getUpdatedAt(),
                category.isDeleted(),
                category.getDeletedAt(),
                category.getVersion(),
                category.getLastModifiedDeviceId()
        );
    }
}
