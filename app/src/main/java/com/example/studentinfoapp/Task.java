package com.example.studentinfoapp;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity(tableName = TaskDbHelper.TABLE_TASKS)
public class Task implements Serializable {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = TaskDbHelper.COL_ID)
    private String id;

    @NonNull
    @ColumnInfo(name = TaskDbHelper.COL_TITLE)
    private String title;

    @ColumnInfo(name = TaskDbHelper.COL_DESCRIPTION)
    private String description;

    @ColumnInfo(name = TaskDbHelper.COL_CATEGORY)
    private String category;

    @ColumnInfo(name = TaskDbHelper.COL_DEADLINE)
    private String deadline;

    @ColumnInfo(name = TaskDbHelper.COL_COMPLETED)
    private boolean isCompleted;

    @ColumnInfo(name = TaskDbHelper.COL_PRIORITY)
    private String priority;

    @ColumnInfo(name = TaskDbHelper.COL_IMAGE_PATH)
    private String imagePath;

    /** Trạng thái UI, không lưu trong DB. */
    @Ignore
    private boolean isSelected;

    @Ignore
    public Task(
            @NonNull String title,
            String description,
            String category,
            String deadline,
            boolean isCompleted,
            String priority) {
        this(UUID.randomUUID().toString(), title, description, category, deadline, isCompleted, priority, null);
        this.isSelected = false;
    }

    /** Constructor tiện lợi (không truyền imagePath); ủy quyền cho constructor đầy đủ. */
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

    /** Constructor đầy đủ các cột DB — Room dùng khi đọc/ghi. */
    public Task(
            @NonNull String id,
            @NonNull String title,
            String description,
            String category,
            String deadline,
            boolean isCompleted,
            String priority,
            String imagePath) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.deadline = deadline;
        this.isCompleted = isCompleted;
        this.priority = priority;
        this.imagePath = imagePath;
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

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return isCompleted == task.isCompleted && isSelected == task.isSelected &&
                Objects.equals(id, task.id) && Objects.equals(title, task.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, isCompleted, isSelected);
    }
}
