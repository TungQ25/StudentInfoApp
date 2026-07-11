package com.example.taskmanager.dto;

import com.example.taskmanager.entity.Category;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CategoryRequest(
        String id,

        @NotBlank
        String name,

        String icon,
        String color,
        boolean pinned,
        int pinnedOrder,
        int sortOrder,
        boolean hidden,
        long createdAt,
        long updatedAt,
        long version,
        String deviceId
) {
    public Category toEntity() {
        Category category = new Category();
        category.setId(id);
        category.setCreatedAt(createdAt);
        applyTo(category);
        return category;
    }

    public void applyTo(Category category) {
        category.setName(name);
        category.setIcon(icon);
        category.setColor(color);
        category.setPinned(pinned);
        category.setPinnedOrder(pinnedOrder);
        category.setSortOrder(sortOrder);
        category.setHidden(hidden);
        category.setUpdatedAt(updatedAt);
    }
}
