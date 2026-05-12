package com.example.studentinfoapp;

import android.content.Context;

import androidx.lifecycle.LiveData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskRepository {
    private static TaskRepository instance;
    private final TaskDao dao;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

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

    /**
     * Luồng quan sát danh sách task; Room tự chạy truy vấn nền và phát giá trị mới khi DB đổi.
     */
    public LiveData<List<Task>> getAllTasksLive() {
        return dao.getAllTasksLive();
    }

    public void addTask(Task task) {
        ioExecutor.execute(() -> dao.insert(task));
    }

    /**
     * Chỉ gọi trên luồng nền (ví dụ từ {@link #ioExecutor} hoặc test).
     */
    public List<Task> getAllTasks() {
        return new ArrayList<>(dao.getAllTasks());
    }

    /**
     * Chỉ gọi trên luồng nền.
     */
    public Optional<Task> getTaskById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(dao.getTaskById(id));
    }

    public void updateTask(Task updatedTask) {
        ioExecutor.execute(() -> dao.update(updatedTask));
    }

    public void deleteTask(String id) {
        ioExecutor.execute(() -> dao.deleteById(id));
    }
}
