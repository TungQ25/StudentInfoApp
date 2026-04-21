package com.example.studentinfoapp;

import java.io.Serializable;

public class Task implements Serializable {
    private String title;
    private String description;
    private String category;
    private String deadline;
    private boolean isCompleted;
    private String priority;

    public Task(String title, String description, String category, String deadline, boolean isCompleted, String priority) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.deadline = deadline;
        this.isCompleted = isCompleted;
        this.priority = priority;
    }

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
}
