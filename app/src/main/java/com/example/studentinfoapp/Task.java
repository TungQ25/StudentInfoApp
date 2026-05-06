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

    // Getters and Setters
    public String getId() { return id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDeadline() { return deadline; }
    public void setDeadline(String deadline) { this.deadline = deadline; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return isCompleted == task.isCompleted &&
                isSelected == task.isSelected &&
                Objects.equals(id, task.id) &&
                Objects.equals(title, task.title) &&
                Objects.equals(description, task.description) &&
                Objects.equals(category, task.category) &&
                Objects.equals(deadline, task.deadline) &&
                Objects.equals(priority, task.priority);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, description, category, deadline, isCompleted, priority, isSelected);
    }
}
