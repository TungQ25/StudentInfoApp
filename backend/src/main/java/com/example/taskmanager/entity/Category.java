package com.example.taskmanager.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "categories")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Category {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @NotBlank
    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "icon", length = 50)
    private String icon;

    @Column(name = "color", length = 50)
    private String color;

    @Column(name = "pinned", nullable = false)
    private boolean pinned = false;

    @Column(name = "pinned_order", nullable = false)
    private int pinnedOrder = -1;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(name = "hidden", nullable = false)
    private boolean hidden = false;

    @Column(name = "created_at", nullable = false)
    private long createdAt = 0L;

    @Column(name = "updated_at", nullable = false)
    private long updatedAt = 0L;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    public String getId() {return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public boolean isPinned() { return pinned; }
    public void setPinned(boolean pinned) { this.pinned = pinned; }
    public int getPinnedOrder() { return pinnedOrder; }
    public void setPinnedOrder(int pinnedOrder) { this.pinnedOrder = pinnedOrder; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public boolean isHidden() { return hidden; }
    public void setHidden(boolean hidden) { this.hidden = hidden; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
}
