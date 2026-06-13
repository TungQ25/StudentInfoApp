package com.example.taskmanagerapp.sync;

import android.content.Context;
import android.util.Log;

import com.example.taskmanagerapp.data.local.TaskDao;
import com.example.taskmanagerapp.data.model.Task;
import com.example.taskmanagerapp.data.remote.RetrofitClient;
import com.example.taskmanagerapp.data.remote.TodoApi;
import com.example.taskmanagerapp.utils.NetworkState;
import com.example.taskmanagerapp.utils.PreferenceHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Response;

public class SyncManager {
    private static final String TAG = "SyncManager";

    private final Context appContext;
    private final TaskDao taskDao;
    private final TodoApi todoApi;
    private final PreferenceHelper preferenceHelper;

    public SyncManager(Context context, TaskDao taskDao) {
        this.appContext = context.getApplicationContext();
        this.taskDao = taskDao;
        this.todoApi = RetrofitClient.getTodoApi();
        this.preferenceHelper = new PreferenceHelper(this.appContext);
    }

    /**
     * Offline-first sync entrypoint. Return false when sync should be retried later.
     */
    public boolean syncNow() {
        // Kiểm tra mạng
        if (!NetworkState.isOnline(appContext)) {
            return false;
        }
        if (!preferenceHelper.hasAuthToken()) {
            return true;
        }

        try {
            // xử lý các task offline đang chờ sync
            if (!pushLocalChanges()) {
                return false;
            }
            return pullRemoteChanges(); // xử lý local xong thì mới pull dữ liệu từ sv về
        } catch (IOException e) {
            Log.e(TAG, "Sync failed with network/server error", e);
            return false;
        }
    }

    /**
     * Đẩy các thay đổi từ local lên server (Create, Update, Delete)
     * @return true nếu đẩy thành công tất cả hoặc lỗi không nghiêm trọng, false nếu cần retry
     * @throws IOException khi có lỗi kết nối mạng
     */
    private boolean pushLocalChanges() throws IOException {
        List<Task> pendingTasks = taskDao.getPendingSyncTasks();
        for (Task task : pendingTasks) {
            Response<?> response; // "?" ko quan trọng trả về kiểu gì, chỉ cần kiểm tra isSuccessful()
            if (task.isDeleted()) {
                response = todoApi.deleteTask(task.getId()).execute();
            } else {
                response = todoApi.updateTask(task.getId(), task).execute();
                if (response.code() == 404) {
                    response = todoApi.createTask(task).execute();
                }
            }

            if (response.isSuccessful() || response.code() == 404 && task.isDeleted()) {
                if (task.isDeleted()) {
                    taskDao.markDeletedSynced(task.getId());
                } else {
                    taskDao.markSynced(task.getId());
                }
            } else if (response.code() == 401) {
                Log.w(TAG, "Authorization failed while syncing task " + task.getId());
                preferenceHelper.clearAuth();
                return false;
            } else if (response.code() >= 500) {
                return false;
            } else {
                Log.w(TAG, "Unhandled sync response " + response.code() + " for task " + task.getId());
            }
        }
        return true;
    }

    /**
     * Lấy dữ liệu từ server về và cập nhật vào local database
     * @return true nếu thành công hoặc không có dữ liệu mới, false nếu cần thử lại sau
     * @throws IOException khi có lỗi kết nối mạng
     */
    private boolean pullRemoteChanges() throws IOException {
        Response<List<Task>> response = todoApi.getAllTasks().execute();
        if (!response.isSuccessful()) {
            if (response.code() == 401) {
                Log.w(TAG, "Authorization failed while fetching tasks");
                preferenceHelper.clearAuth();
                return false;
            }
            return response.code() != 404 && response.code() < 500;
        }

        // Ko có dữ liệu gì thì ko có gì để cập nhật -> true
        List<Task> remoteTasks = response.body();
        if (remoteTasks == null) {
            return true;
        }

        List<Task> mapped = new ArrayList<>();
        for (Task remote : remoteTasks) {
            Task task = Task.fromRemote(remote);
            Task local = taskDao.getTaskById(task.getId());

            // Nếu task local đã xóa và chưa sync xóa thì không lấy task từ server xuống nữa
            if (local != null && local.isDeleted()) {
                continue;
            }
            // Nếu task local mới hơn remote và chưa sync thì không ghi đè bằng dữ liệu cũ từ server
            if (local != null && !local.isSynced() && local.getUpdatedAt() > task.getUpdatedAt()) {
                continue;
            }
            mapped.add(task);
        }

        if (!mapped.isEmpty()) {
            taskDao.upsertAll(mapped);
        }
        return true;
    }
}
