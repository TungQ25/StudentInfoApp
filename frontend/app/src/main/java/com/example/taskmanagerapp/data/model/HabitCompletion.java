package com.example.taskmanagerapp.data.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.example.taskmanagerapp.contract.TaskContract;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

@Entity(tableName = TaskContract.TABLE_HABIT_COMPLETIONS)
public class HabitCompletion implements Serializable {

    @PrimaryKey
    @NonNull
    @SerializedName("id")
    @ColumnInfo(name = TaskContract.COL_ID)
    private String id;

    @NonNull
    @SerializedName("habitId")
    @ColumnInfo(name = TaskContract.COL_HABIT_ID)
    private String habitId;

    @NonNull
    @SerializedName("userId")
    @ColumnInfo(name = TaskContract.COL_USER_ID)
    private String userId;

    @NonNull
    @SerializedName("periodKey")
    @ColumnInfo(name = TaskContract.COL_PERIOD_KEY)
    private String periodKey;

    @SerializedName("completedAt")
    @ColumnInfo(name = TaskContract.COL_COMPLETED_AT, defaultValue = "0")
    private long completedAt;

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
    public HabitCompletion(@NonNull String id, @NonNull String habitId, @NonNull String userId, @NonNull String periodKey) {
        this(id, habitId, userId, periodKey, System.currentTimeMillis(), System.currentTimeMillis(), false, false);
    }

    public HabitCompletion(@NonNull String id, @NonNull String habitId, @NonNull String userId, @NonNull String periodKey, long completedAt, long updatedAt, boolean synced, boolean deleted) {
        this.id = id;
        this.habitId = habitId;
        this.userId = userId;
        this.periodKey = periodKey;
        this.completedAt = completedAt;
        this.updatedAt = updatedAt;
        this.synced = synced;
        this.deleted = deleted;
    }

    @NonNull public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
    @NonNull public String getHabitId() { return habitId; }
    public void setHabitId(@NonNull String habitId) { this.habitId = habitId; }
    @NonNull public String getUserId() { return userId; }
    public void setUserId(@NonNull String userId) { this.userId = userId; }
    @NonNull public String getPeriodKey() { return periodKey; }
    public void setPeriodKey(@NonNull String periodKey) { this.periodKey = periodKey; }
    public long getCompletedAt() { return completedAt; }
    public void setCompletedAt(long completedAt) { this.completedAt = completedAt; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
    public boolean isSynced() { return synced; }
    public void setSynced(boolean synced) { this.synced = synced; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }

    public void markLocalChange() {
        long now = System.currentTimeMillis();
        if (completedAt <= 0) {
            completedAt = now;
        }
        updatedAt = now;
        synced = false;
        deleted = false;
    }

    public void markDeletedLocal() {
        updatedAt = System.currentTimeMillis();
        synced = false;
        deleted = true;
    }

    public static HabitCompletion fromRemote(HabitCompletion remote) {
        return new HabitCompletion(
                remote.getId(),
                remote.getHabitId(),
                remote.getUserId() == null ? "" : remote.getUserId(),
                remote.getPeriodKey(),
                remote.getCompletedAt(),
                remote.getUpdatedAt(),
                true,
                remote.isDeleted()
        );
    }
}
