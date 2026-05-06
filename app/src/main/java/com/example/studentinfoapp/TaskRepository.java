package com.example.studentinfoapp;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TaskRepository {
    private static TaskRepository instance;
    private final List<Task> taskList;

    private TaskRepository() {
        taskList = new ArrayList<>();
    }

    public static synchronized TaskRepository getInstance() {
        if (instance == null) {
            instance = new TaskRepository();
        }
        return instance;
    }

    public void addTask(Task task) {
        taskList.add(task);
    }

    // Read (All)
    public List<Task> getAllTasks() {
        return new ArrayList<>(taskList);
    }

    // Read (By ID)
    public Optional<Task> getTaskById(String id) {
        return taskList.stream()
                .filter(task -> task.getId().equals(id))
                .findFirst();
    }

    public void updateTask(Task updatedTask) {
        for (int i = 0; i < taskList.size(); i++) {
            if (taskList.get(i).getId().equals(updatedTask.getId())) {
                taskList.set(i, updatedTask);
                return;
            }
        }
    }

    public void deleteTask(String id) {
        taskList.removeIf(task -> task.getId().equals(id));
    }
}
