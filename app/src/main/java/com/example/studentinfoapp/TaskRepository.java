package com.example.studentinfoapp;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TaskRepository {
    private static TaskRepository instance;
    private final TaskDao dao;

    private TaskRepository(Context appContext) {
        TaskDbHelper helper = new TaskDbHelper(appContext.getApplicationContext());
        this.dao = new TaskDao(helper);
    }

    /**
     * Khởi tạo lần đầu cần {@link Context} (nên dùng application context).
     */
    public static synchronized TaskRepository getInstance(Context context) {
        if (instance == null) {
            instance = new TaskRepository(context.getApplicationContext());
        }
        return instance;
    }

    public void addTask(Task task) {
        dao.insertTask(task);
    }

    public List<Task> getAllTasks() {
        return new ArrayList<>(dao.getAllTasks());
    }

    public Optional<Task> getTaskById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        for (Task task : dao.getAllTasks()) {
            if (id.equals(task.getId())) {
                return Optional.of(task);
            }
        }
        return Optional.empty();
    }

    public void updateTask(Task updatedTask) {
        dao.updateTask(updatedTask);
    }

    public void deleteTask(String id) {
        dao.deleteTask(id);
    }
}
