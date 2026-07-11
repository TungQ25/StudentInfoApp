package com.example.taskmanagerapp.data.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.example.taskmanagerapp.contract.TaskContract;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.UUID;

@Entity(tableName = TaskContract.TABLE_CATEGORIES)
public class Category implements Serializable {

    @PrimaryKey
    @NonNull
    @SerializedName("id")
    @ColumnInfo(name = TaskContract.COL_ID)
    private String id;

    @NonNull
    @SerializedName("userId")
    @ColumnInfo(name = TaskContract.COL_USER_ID)
    private String userId;

    @NonNull
    @SerializedName("name")
    private String name;

    @SerializedName("icon")
    private String icon;

    @SerializedName("color")
    private String color;

    @SerializedName("pinned")
    private boolean pinned;

    @SerializedName("pinnedOrder")
    private int pinnedOrder;

    @SerializedName("sortOrder")
    private int sortOrder;

    @SerializedName("hidden")
    private boolean hidden;

    @SerializedName("createdAt")
    private long createdAt;

    @SerializedName("updatedAt")
    private long updatedAt;

    @SerializedName("synced")
    private boolean synced;

    @SerializedName("deleted")
    private boolean deleted;

    // TODO: Thêm permanentDeletePending cho category
    @Ignore
    public Category(@NonNull String name, String icon, String userId, int sortOrder) {
        this(UUID.randomUUID().toString(), userId == null ? "" : userId, name, icon, null, false, -1, sortOrder, false, System.currentTimeMillis(), System.currentTimeMillis(), false, false);
    }

    public Category(@NonNull String id, @NonNull String userId, @NonNull String name, String icon, String color, boolean pinned, int pinnedOrder, int sortOrder, boolean hidden, long createdAt, long updatedAt, boolean synced, boolean deleted) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.icon = icon;
        this.color = color;
        this.pinned = pinned;
        this.pinnedOrder = pinnedOrder;
        this.sortOrder = sortOrder;
        this.hidden = hidden;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.synced = synced;
        this.deleted = deleted;
    }

    @NonNull public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
    @NonNull public String getUserId() { return userId; }
    public void setUserId(@NonNull String userId) { this.userId = userId; }
    @NonNull public String getName() { return name; }
    public void setName(@NonNull String name) { this.name = name; }
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
    public boolean isSynced() { return synced; }
    public void setSynced(boolean synced) { this.synced = synced; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }

    public void markLocalChange() {
        updatedAt = System.currentTimeMillis();
        synced = false;
        deleted = false;
    }

    public void markDeletedLocal() {
        updatedAt = System.currentTimeMillis();
        synced = false;
        deleted = true;
    }

    public static Category fromRemote(Category remote) {
        return new Category(
                remote.getId(),
                remote.getUserId() == null ? "" : remote.getUserId(),
                remote.getName() == null ? "Category" : remote.getName(),
                remote.getIcon(),
                remote.getColor(),
                remote.isPinned(),
                remote.getPinnedOrder(),
                remote.getSortOrder(),
                remote.isHidden(),
                remote.getCreatedAt(),
                remote.getUpdatedAt(),
                true,
                remote.isDeleted()
        );
    }
}
