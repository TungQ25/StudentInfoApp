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

@Entity(tableName = TaskContract.TABLE_HABITS)
public class Habit implements Serializable {

    public static final String FREQUENCY_DAILY = "daily";
    public static final String FREQUENCY_WEEKLY = "weekly";
    public static final String FREQUENCY_MONTHLY = "monthly";

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
    @SerializedName("title")
    @ColumnInfo(name = TaskContract.COL_TITLE)
    private String title;

    @SerializedName("groupName")
    @ColumnInfo(name = TaskContract.COL_GROUP_NAME)
    private String groupName;

    @SerializedName("frequency")
    @ColumnInfo(name = TaskContract.COL_FREQUENCY)
    private String frequency;

    @SerializedName("totalDays")
    @ColumnInfo(name = TaskContract.COL_TOTAL_DAYS, defaultValue = "0")
    private int totalDays;

    @SerializedName("color")
    @ColumnInfo(name = TaskContract.COL_COLOR, defaultValue = "0")
    private int color;

    @SerializedName("sortOrder")
    @ColumnInfo(name = TaskContract.COL_SORT_ORDER, defaultValue = "0")
    private int sortOrder;

    @SerializedName("createdAt")
    @ColumnInfo(name = TaskContract.COL_CREATED_AT, defaultValue = "0")
    private long createdAt;

    @SerializedName("updatedAt")
    @ColumnInfo(name = TaskContract.COL_UPDATED_AT, defaultValue = "0")
    private long updatedAt;

    @SerializedName("synced")
    @ColumnInfo(name = TaskContract.COL_SYNCED, defaultValue = "1")
    private boolean synced;

    @SerializedName("deleted")
    @ColumnInfo(name = TaskContract.COL_DELETED, defaultValue = "0")
    private boolean deleted;

    @Ignore
    public Habit(@NonNull String title, String groupName, String frequency, int totalDays, int color, String userId, int sortOrder) {
        this(UUID.randomUUID().toString(), userId == null ? "" : userId, title, groupName, normalizeFrequency(frequency), totalDays, color, sortOrder, System.currentTimeMillis(), System.currentTimeMillis(), false, false);
    }

    public Habit(@NonNull String id, @NonNull String userId, @NonNull String title, String groupName, String frequency, int totalDays, int color, int sortOrder, long createdAt, long updatedAt, boolean synced, boolean deleted) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.groupName = groupName;
        this.frequency = normalizeFrequency(frequency);
        this.totalDays = Math.max(0, totalDays);
        this.color = color;
        this.sortOrder = sortOrder;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.synced = synced;
        this.deleted = deleted;
    }

    @NonNull public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
    @NonNull public String getUserId() { return userId; }
    public void setUserId(@NonNull String userId) { this.userId = userId; }
    @NonNull public String getTitle() { return title; }
    public void setTitle(@NonNull String title) { this.title = title; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = normalizeFrequency(frequency); }
    public int getTotalDays() { return totalDays; }
    public void setTotalDays(int totalDays) { this.totalDays = Math.max(0, totalDays); }
    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
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

    public static Habit fromRemote(Habit remote) {
        return new Habit(
                remote.getId(),
                remote.getUserId() == null ? "" : remote.getUserId(),
                remote.getTitle() == null ? "Habit" : remote.getTitle(),
                remote.getGroupName(),
                normalizeFrequency(remote.getFrequency()),
                remote.getTotalDays(),
                remote.getColor(),
                remote.getSortOrder(),
                remote.getCreatedAt(),
                remote.getUpdatedAt(),
                true,
                remote.isDeleted()
        );
    }

    public static String normalizeFrequency(String frequency) {
        if (FREQUENCY_WEEKLY.equalsIgnoreCase(frequency)) {
            return FREQUENCY_WEEKLY;
        }
        if (FREQUENCY_MONTHLY.equalsIgnoreCase(frequency)) {
            return FREQUENCY_MONTHLY;
        }
        return FREQUENCY_DAILY;
    }
}
