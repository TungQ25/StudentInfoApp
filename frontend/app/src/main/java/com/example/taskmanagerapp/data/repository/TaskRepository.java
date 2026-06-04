package com.example.taskmanagerapp.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.taskmanagerapp.data.local.AppDatabase;
import com.example.taskmanagerapp.data.local.TaskDao;
import com.example.taskmanagerapp.data.model.Task;
import com.example.taskmanagerapp.sync.SyncManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository quản lý dữ liệu Task, kết nối giữa UI và các nguồn dữ liệu (Room DB, SyncManager).
 * Hỗ trợ các thao tác CRUD và đồng bộ hóa dữ liệu.
 */
public class TaskRepository {
    private static TaskRepository instance;
    private final TaskDao dao;
    private final SyncManager syncManager;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    private TaskRepository(Context appContext) {
        AppDatabase db = AppDatabase.getInstance(appContext.getApplicationContext());
        this.dao = db.taskDao();
        this.syncManager = new SyncManager(appContext.getApplicationContext(), dao);
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
        ioExecutor.execute(() -> {
            task.markLocalChange();
            dao.insert(task);
            syncManager.syncNow();
        });
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
        ioExecutor.execute(() -> {
            updatedTask.markLocalChange();
            dao.update(updatedTask);
            syncManager.syncNow();
        });
    }

    public void deleteTask(String id) {
        ioExecutor.execute(() -> {
            int updated = dao.markDeletedForSync(id, System.currentTimeMillis());
            if (updated == 0) {
                dao.deleteById(id);
            }
            syncManager.syncNow();
        });
    }

    public void syncTasks() {
        ioExecutor.execute(syncManager::syncNow);
    }
}
