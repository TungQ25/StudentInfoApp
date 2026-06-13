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

    @ColumnInfo(name = TaskContract.COL_CATEGORY)
    private String category;

    @ColumnInfo(name = TaskContract.COL_DEADLINE)
    private String deadline;

    @SerializedName("completed")
    @ColumnInfo(name = TaskContract.COL_COMPLETED)
    private boolean isCompleted;

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

    @Ignore
    @SerializedName("userId")
    private String userId;

    /** Trang thai UI, khong luu trong DB. */
    @Ignore
    private boolean isSelected;

    /** Tạo task mới không có id. */
    @Ignore
    public Task(
            @NonNull String title,
            String description,
            String category,
            String deadline,
            boolean isCompleted,
            String priority) {
        this(UUID.randomUUID().toString(), title, description, category, deadline, isCompleted, priority, null);
    }

    /** Tạo task mới có id. */
    @Ignore
    public Task(
            @NonNull String id,
            @NonNull String title,
            String description,
            String category,
            String deadline,
            boolean isCompleted,
            String priority) {
        this(id, title, description, category, deadline, isCompleted, priority, null);
    }

    /** Tạo task mới có id và title. */
    @Ignore
    public Task(
            @NonNull String id,
            @NonNull String title,
            String description,
            String category,
            String deadline,
            boolean isCompleted,
            String priority,
            String imagePath) {
        this(
                id,
                title,
                description,
                category,
                deadline,
                isCompleted,
                priority,
                imagePath,
                System.currentTimeMillis(),
                false,
                false);
    }

    /** Tạo task mới với tất cả các cột DB - Room dùng khi đọc/ghi. */
    public Task(
            @NonNull String id,
            @NonNull String title,
            String description,
            String category,
            String deadline,
            boolean isCompleted,
            String priority,
            String imagePath,
            long updatedAt,
            boolean synced,
            boolean deleted ) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.deadline = deadline;
        this.isCompleted = isCompleted;
        this.priority = priority;
        this.imagePath = imagePath;
        this.updatedAt = updatedAt;
        this.synced = synced;
        this.deleted = deleted;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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
    }

    public void markDeletedLocal() {
        updatedAt = System.currentTimeMillis();
        synced = false;
        deleted = true;
    }

    public static Task fromRemote(Task remote) {
        String id = remote.getId() == null ? UUID.randomUUID().toString() : remote.getId();
        String title = remote.getTitle() == null ? "" : remote.getTitle();
        Task task = new Task(
                id,
                title,
                remote.getDescription(),
                remote.getCategory() == null ? "All" : remote.getCategory(),
                remote.getDeadline(),
                remote.isCompleted(),
                remote.getPriority() == null ? "Low" : remote.getPriority(),
                remote.getImagePath(),
                System.currentTimeMillis(),
                true,
                false);
        task.setUserId(remote.getUserId());
        return task;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return isCompleted == task.isCompleted
                && updatedAt == task.updatedAt
                && synced == task.synced
                && deleted == task.deleted
                && isSelected == task.isSelected
                && Objects.equals(id, task.id)
                && Objects.equals(title, task.title)
                && Objects.equals(description, task.description)
                && Objects.equals(category, task.category)
                && Objects.equals(deadline, task.deadline)
                && Objects.equals(priority, task.priority)
                && Objects.equals(imagePath, task.imagePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, description, category, deadline, isCompleted, priority,
                imagePath, updatedAt, synced, deleted, isSelected);
    }

    @Override
    public String toString() {
        return "Task{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", category='" + category + '\'' +
                ", deadline='" + deadline + '\'' +
                ", isCompleted=" + isCompleted +
                ", priority='" + priority + '\'' +
                ", imagePath='" + imagePath + '\'' +
                ", updatedAt=" + updatedAt +
                ", synced=" + synced +
                ", deleted=" + deleted +
                '}';
    }
}
