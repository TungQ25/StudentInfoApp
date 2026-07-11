package com.example.taskmanagerapp.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.taskmanagerapp.data.local.AppDatabase;
import com.example.taskmanagerapp.data.local.TaskDao;
import com.example.taskmanagerapp.data.model.Task;
import com.example.taskmanagerapp.sync.SyncManager;
import com.example.taskmanagerapp.utils.PreferenceHelper;

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
    private final PreferenceHelper preferenceHelper;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    private TaskRepository(Context appContext) {
        Context applicationContext = appContext.getApplicationContext();
        AppDatabase db = AppDatabase.getInstance(applicationContext);
        this.dao = db.taskDao();
        this.syncManager = new SyncManager(applicationContext);
        this.preferenceHelper = new PreferenceHelper(applicationContext);
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
        return dao.getAllTasksLive(currentUserId());
    }

    public void addTask(Task task) {
        ioExecutor.execute(() -> {
            task.setUserId(currentUserId());
            task.markLocalChange();
            dao.insert(task);
            syncManager.syncNow();
        });
    }

    /**
     * Chỉ gọi trên luồng nền (ví dụ từ {@link #ioExecutor} hoặc test).
     */
    public List<Task> getAllTasks() {
        return new ArrayList<>(dao.getAllTasks(currentUserId()));
    }

    /**
     * Chỉ gọi trên luồng nền.
     */
    public Optional<Task> getTaskById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(dao.getTaskById(id, currentUserId()));
    }

    public void updateTask(Task updatedTask) {
        ioExecutor.execute(() -> {
            updatedTask.setUserId(currentUserId());
            updatedTask.markLocalChange();
            dao.update(updatedTask);
            syncManager.syncNow();
        });
    }

    public void deleteTask(String id) {
        ioExecutor.execute(() -> {
            String userId = currentUserId();
            int updated = dao.markDeletedForSync(id, userId, System.currentTimeMillis());
            if (updated == 0) {
                dao.deleteById(id, userId);
            }
            syncManager.syncNow();
        });
    }

    public void restoreTask(String id) {
        ioExecutor.execute(() -> {
            if (id == null) {
                return;
            }

            String userId = currentUserId();
            int updated = dao.markRestoredForSync(id, userId, System.currentTimeMillis());
            if (updated > 0) {
                syncManager.syncNow();
            }
        });
    }

    public void permanentlyDeleteTask(String id) {
        ioExecutor.execute(() -> {
            if (id == null) {
                return;
            }

            String userId = currentUserId();
            int updated = dao.markPermanentDeletePending(id, userId, System.currentTimeMillis());
            if (updated > 0) {
                syncManager.syncNow();
            }
        });
    }

    public void emptyTrash() {
        ioExecutor.execute(() -> {
            String userId = currentUserId();
            int updated = dao.markAllTrashPermanentDeletePending(userId, System.currentTimeMillis());
            if (updated > 0) {
                preferenceHelper.setTaskEmptyTrashPending(true);
                syncManager.syncNow();
            }
        });
    }

    public void syncTasks() {
        ioExecutor.execute(syncManager::syncNow);
    }

    private String currentUserId() {
        String userId = preferenceHelper.getAuthUserId();
        return userId == null ? "" : userId;
    }
}
