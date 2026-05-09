package com.example.studentinfoapp;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class Task implements Serializable {
    private final String id;
    private String title;
    private String description;
    private String category;
    private String deadline;
    private boolean isCompleted;
    private String priority;
    private boolean isSelected;

    public Task(String title, String description, String category, String deadline, boolean isCompleted, String priority) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.description = description;
        this.category = category;
        this.deadline = deadline;
        this.isCompleted = isCompleted;
        this.priority = priority;
        this.isSelected = false;
    }

    // Constructor dùng để update (giữ nguyên ID)
    public Task(String id, String title, String description, String category, String deadline, boolean isCompleted, String priority) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.deadline = deadline;
        this.isCompleted = isCompleted;
        this.priority = priority;
        this.isSelected = false;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public String getDeadline() { return deadline; }
    public boolean isCompleted() { return isCompleted; }
    public String getPriority() { return priority; }
    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }
    public void setCompleted(boolean completed) { isCompleted = completed; }

    private String imagePath; 
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

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
