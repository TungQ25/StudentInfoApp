package com.example.taskmanagerapp.data.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.example.taskmanagerapp.contract.TaskContract;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity(tableName = TaskContract.TABLE_TASKS)
public class Task implements Serializable {

    @PrimaryKey
    @NonNull
    @SerializedName("id")
    @ColumnInfo(name = TaskContract.COL_ID)
    private String id;

    @NonNull
    @SerializedName("title")
    @ColumnInfo(name = TaskContract.COL_TITLE)
    private String title;

    @SerializedName("description")
    @ColumnInfo(name = TaskContract.COL_DESCRIPTION)
    private String description;

    @SerializedName("categoryId")
    @ColumnInfo(name = TaskContract.COL_CATEGORY_ID)
    private String categoryId;

    @ColumnInfo(name = TaskContract.COL_DEADLINE)
    private String deadline;

    @SerializedName("completed")
    @ColumnInfo(name = TaskContract.COL_COMPLETED)
    private boolean isCompleted;

    @SerializedName("wontDo")
    @ColumnInfo(name = TaskContract.COL_WONT_DO, defaultValue = "0")
    private boolean wontDo;

    @ColumnInfo(name = TaskContract.COL_PRIORITY)
    private String priority;

    @ColumnInfo(name = TaskContract.COL_IMAGE_PATH)
    private String imagePath;

    @ColumnInfo(name = TaskContract.COL_UPDATED_AT, defaultValue = "0")
    private long updatedAt;

    @ColumnInfo(name = TaskContract.COL_SYNCED, defaultValue = "1")
    private boolean synced;

    @ColumnInfo(name = TaskContract.COL_DELETED, defaultValue = "0")
    private boolean deleted;

    @ColumnInfo(name = TaskContract.COL_PERMANENT_DELETE_PENDING, defaultValue = "0")
    private boolean permanentDeletePending;

    @ColumnInfo(name = TaskContract.COL_REMOTE_EXISTS, defaultValue = "0")
    private boolean remoteExists;

    @SerializedName("userId")
    @ColumnInfo(name = TaskContract.COL_USER_ID)
    private String userId;

    /** Trang thai UI, khong luu trong DB. */
    @Ignore
    private boolean isSelected;

    /** Tạo task mới không có id. */
    @Ignore
    public Task(
            @NonNull String title,
            String description,
            String categoryId,
            String deadline,
            boolean isCompleted,
            String priority) {
        this(UUID.randomUUID().toString(), title, description, categoryId, deadline, isCompleted, priority, null);
    }

    /** Tạo task mới có id. */
    @Ignore
    public Task(
            @NonNull String id,
            @NonNull String title,
            String description,
            String categoryId,
            String deadline,
            boolean isCompleted,
            String priority) {
        this(id, title, description, categoryId, deadline, isCompleted, priority, null);
    }

    /** Tạo task mới có id và title. */
    @Ignore
    public Task(
            @NonNull String id,
            @NonNull String title,
            String description,
            String categoryId,
            String deadline,
            boolean isCompleted,
            String priority,
            String imagePath) {
        this(
                id,
                title,
                description,
                categoryId,
                deadline,
                isCompleted,
                false,
                priority,
                imagePath,
                System.currentTimeMillis(),
                false,
                false,
                false,
                false,
                null);
    }

    /** Tạo task mới với tất cả các cột DB - Room dùng khi đọc/ghi. */
    public Task(
            @NonNull String id,
            @NonNull String title,
            String description,
            String categoryId,
            String deadline,
            boolean isCompleted,
            boolean wontDo,
            String priority,
            String imagePath,
            long updatedAt,
            boolean synced,
            boolean deleted,
            boolean permanentDeletePending,
            boolean remoteExists,
            String userId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.categoryId = categoryId;
        this.deadline = deadline;
        this.isCompleted = isCompleted;
        this.wontDo = wontDo;
        this.priority = priority;
        this.imagePath = imagePath;
        this.updatedAt = updatedAt;
        this.synced = synced;
        this.deleted = deleted;
        this.permanentDeletePending = permanentDeletePending;
        this.remoteExists = remoteExists;
        this.userId = userId;
        this.isSelected = false;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    public void setTitle(@NonNull String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getDeadline() {
        return deadline;
    }

    public void setDeadline(String deadline) {
        this.deadline = deadline;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
        if (completed) {
            wontDo = false;
        }
    }

    public boolean isWontDo() {
        return wontDo;
    }

    public void setWontDo(boolean wontDo) {
        this.wontDo = wontDo;
        if (wontDo) {
            isCompleted = false;
        }
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isSynced() {
        return synced;
    }

    public void setSynced(boolean synced) {
        this.synced = synced;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public boolean isPermanentDeletePending() {
        return permanentDeletePending;
    }

    public void setPermanentDeletePending(boolean permanentDeletePending) {
        this.permanentDeletePending = permanentDeletePending;
    }

    public boolean isRemoteExists() {
        return remoteExists;
    }

    public void setRemoteExists(boolean remoteExists) {
        this.remoteExists = remoteExists;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public void markLocalChange() {
        updatedAt = System.currentTimeMillis();
        synced = false;
        deleted = false;
        permanentDeletePending = false;
    }

    public void markDeletedLocal() {
        updatedAt = System.currentTimeMillis();
        synced = false;
        deleted = true;
        permanentDeletePending = false;
    }

    /**
     * Chuyển một Task lấy từ server về thành Task local để lưu vào Room.
     * @param remote The task data from the remote source.
     * @return A new Task instance configured for local storage.
     */
    public static Task fromRemote(Task remote) {
        if (remote == null) return null;
        return new Task(
                remote.getId() == null ? UUID.randomUUID().toString() : remote.getId(),
                remote.getTitle() == null ? "" : remote.getTitle(),
                remote.getDescription(),
                remote.getCategoryId(),
                remote.getDeadline(),
                remote.isCompleted(),
                remote.isWontDo(),
                remote.getPriority() == null || remote.getPriority().trim().isEmpty() ? "None" : remote.getPriority(),
                remote.getImagePath(),
                remote.getUpdatedAt(),
                true,
                false,
                false,
                true,
                remote.getUserId());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return isCompleted == task.isCompleted
                && wontDo == task.wontDo
                && updatedAt == task.updatedAt
                && synced == task.synced
                && deleted == task.deleted
                && permanentDeletePending == task.permanentDeletePending
                && remoteExists == task.remoteExists
                && isSelected == task.isSelected
                && Objects.equals(id, task.id)
                && Objects.equals(title, task.title)
                && Objects.equals(description, task.description)
                && Objects.equals(categoryId, task.categoryId)
                && Objects.equals(deadline, task.deadline)
                && Objects.equals(priority, task.priority)
                && Objects.equals(imagePath, task.imagePath)
                && Objects.equals(userId, task.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, description, categoryId, deadline, isCompleted, wontDo,
                priority, imagePath, updatedAt, synced, deleted, permanentDeletePending, remoteExists, userId, isSelected);
    }
}
