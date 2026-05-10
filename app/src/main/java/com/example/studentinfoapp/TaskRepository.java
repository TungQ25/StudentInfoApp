package com.example.studentinfoapp;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TaskRepository {
    private static TaskRepository instance;
    private final TaskDao dao;

    private TaskRepository(Context appContext) {
        AppDatabase db = AppDatabase.getInstance(appContext.getApplicationContext());
        this.dao = db.taskDao();
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
        dao.insert(task);
    }

    public List<Task> getAllTasks() {
        return new ArrayList<>(dao.getAllTasks());
    }

    public Optional<Task> getTaskById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(dao.getTaskById(id));
    }

    public void updateTask(Task updatedTask) {
        dao.update(updatedTask);
    }

    public void deleteTask(String id) {
        dao.deleteById(id);
    }
}
