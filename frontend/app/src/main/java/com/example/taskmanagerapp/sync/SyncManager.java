package com.example.taskmanagerapp.sync;

import android.content.Context;
import android.util.Log;

import com.example.taskmanagerapp.data.local.AppDatabase;
import com.example.taskmanagerapp.data.local.CategoryDao;
import com.example.taskmanagerapp.data.local.TaskDao;
import com.example.taskmanagerapp.data.model.Category;
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
    private final CategoryDao categoryDao;
    private final TodoApi todoApi;
    private final PreferenceHelper preferenceHelper;

    public SyncManager(Context context) {
        this.appContext = context.getApplicationContext();
        this.taskDao = taskDao;
        AppDatabase db = AppDatabase.getInstance(this.appContext);
        this.taskDao = db.taskDao();
        this.categoryDao = db.categoryDao();
        this.todoApi = RetrofitClient.getTodoApi();
        this.preferenceHelper = new PreferenceHelper(this.appContext);
    }

    /**
     * Offline-first sync entrypoint. Return false when sync should be retried later.
     */
    public boolean syncNow() {
        // Kiểm tra mạng
        if (!NetworkState.isOnline(appContext)) {
            Log.d(TAG, "Skip sync: device is offline");
            return false;
        }
        if (!preferenceHelper.hasAuthToken()) {
            Log.d(TAG, "Skip sync: user is not authenticated");
            return true;
        }

        try {
            if (!pushPendingPermanentTaskDeletes()) return false;
            if (!pushLocalCategoryChanges()) return false;
            if (!pushLocalTaskChanges()) return false;
            if (!pushLocalHabitChanges()) return false; // commit sau
            if (!pushLocalHabitCompletionChanges()) return false; // commit sau
            if (!pullRemoteCategories()) return false;
            if (!flushPendingEmptyTrash()) return false;
            if (!pullRemoteTasks()) return false;
            if (!pullRemoteHabits()) return false; // commit sau
            return pullRemoteHabitCompletions(); // commit sau
        } catch (IOException e) {
            Log.e(TAG, "Sync failed with network/server error", e);
            return false;
        }
    }

    /**
     * Đẩy các task đang chờ xoá cứng lên server
     * @return true nếu thành công, false nếu cần thử lại sau
     * @throws IOException
     */
    private boolean pushPendingPermanentTaskDeletes() throws IOException {
        String userId = currentUserId();
        List<Task> pendingTasks = taskDao.getPendingPermanentDeleteTasks(userId);
        Log.d(TAG, "Pending permanent task deletes for sync: " + pendingTasks.size());
        for (Task task : pendingTasks) {
            task.setUserId(userId); // gán user lại cho chắc chắn
            if (deleteRemoteTaskPermanently(task)) { // sv xử lý xoá cứng task
                taskDao.deletePermanently(task.getId(), userId); // thành công thì xoá luôn task ở local
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * Xử lý xoá cứng task vĩnh viễn
     * @param task
     * @return
     * @throws IOException
     */
    private boolean deleteRemoteTaskPermanently(Task task) throws IOException {
        Response<Void> response = todoApi.permanentlyDeleteTask(task.getId()).execute(); // gọi api xoá cứng
        Log.d(TAG, "DELETE /api/tasks/" + task.getId() + "/permanent -> " + response.code());
        if (response.isSuccessful() || response.code() == 404) { // 404: Task không tồn tại trên server -> xoá thành công
            return true;
        }
        if (response.code() == 401) {
            preferenceHelper.clearAuth();
            return false;
        }

        // Task có thể chưa được soft-delete trên server (TH mất mạng nhưng Local đã xoá cứng task)
        if (response.code() == 400) {
            Response<Task> softDeleteResponse = todoApi.deleteTask(task.getId()).execute(); // xoá mềm
            Log.d(TAG, "DELETE /api/tasks/" + task.getId() + " -> " + softDeleteResponse.code());
            // xoá mềm thành công thì thử lại xoá cứng trên sv
            if (softDeleteResponse.isSuccessful()) {
                Response<Void> retryResponse = todoApi.permanentlyDeleteTask(task.getId()).execute();
                Log.d(TAG, "DELETE /api/tasks/" + task.getId() + "/permanent retry -> " + retryResponse.code());
                if (retryResponse.code() == 401) {
                    preferenceHelper.clearAuth();
                    return false;
                }
                return retryResponse.isSuccessful() || retryResponse.code() == 404;
            }
            if (softDeleteResponse.code() == 404) { // Task không tồn tại trên server -> xoá thành công
                return true;
            }
            if (softDeleteResponse.code() == 401) {
                preferenceHelper.clearAuth();
            }
        }
        return false;
    }

    /**
     * Xoá cứng toàn bộ task trong thùng rác
     * @return
     * @throws IOException
     */
    private boolean flushPendingEmptyTrash() throws IOException {
        if (!preferenceHelper.isTaskEmptyTrashPending()) {
            return true;
        }

        String userId = currentUserId();
        Response<Void> response = todoApi.emptyTrash().execute();
        Log.d(TAG, "DELETE /api/tasks/trash -> " + response.code());
        if (response.isSuccessful()) {
            taskDao.deleteAllTrash(userId);
            preferenceHelper.setTaskEmptyTrashPending(false);
            return true;
        }
        if (response.code() == 401) {
            preferenceHelper.clearAuth();
        }
        return false;
    }

    private boolean pushLocalCategoryChanges() throws IOException {
        String userId = currentUserId();
        List<Category> pendingCategories = categoryDao.getPendingSyncCategories(userId);
        Log.d(TAG, "Pending local categories for sync: " + pendingCategories.size());
        for (Category category : pendingCategories) {
            category.setUserId(userId);
            Response<?> response = pushCategory(category);
            if (response.isSuccessful() || (response.code() == 404 && category.isDeleted())) {
                if (category.isDeleted()) {
                    categoryDao.markDeletedSynced(category.getId(), userId);
                } else {
                    categoryDao.markSynced(category.getId(), userId);
                }
            } else if (response.code() == 401) {
                preferenceHelper.clearAuth();
                return false;
            } else if (response.code() >= 500) {
                return false;
            }
        }
        return true;
    }

    private Response<?> pushCategory(Category category) throws IOException {
        if (category.isDeleted()) {
            Response<Category> response = todoApi.deleteCategory(category.getId()).execute();
            Log.d(TAG, "DELETE /api/categories/" + category.getId() + " -> " + response.code());
            return response;
        }

        Response<Category> response = todoApi.updateCategory(category.getId(), category).execute();
        Log.d(TAG, "PUT /api/categories/" + category.getId() + " -> " + response.code());
        if (response.code() == 404) {
            Response<Category> createResponse = todoApi.createCategory(category).execute();
            Log.d(TAG, "POST /api/categories -> " + createResponse.code());
            return createResponse;
        }
        return response;
    }

    /**
     * Đẩy các thay đổi từ local lên server (Create, Update, Delete)
     * @return true nếu đẩy thành công tất cả hoặc lỗi không nghiêm trọng, false nếu cần retry
     * @throws IOException khi có lỗi kết nối mạng
     */
    private boolean pushLocalTaskChanges() throws IOException {
        String userId = currentUserId();
        List<Task> pendingTasks = taskDao.getPendingSyncTasks(userId);
        Log.d(TAG, "Pending local tasks for sync: " + pendingTasks.size());
        for (Task task : pendingTasks) {
            task.setUserId(userId);
            Response<?> response = pushTask(task); // "?" ko quan trọng trả về kiểu gì, chỉ cần kiểm tra isSuccessful()

            if (response.isSuccessful() || (response.code() == 404 && task.isDeleted())) {
                if (task.isDeleted()) {
                    taskDao.markDeletedSynced(task.getId(), userId);
                } else {
                    taskDao.markSynced(task.getId(), userId);
                }
            } else if (response.code() == 401) {
                preferenceHelper.clearAuth();
                return false;
            } else if (response.code() >= 500) {
                return false;
            }
        }
        return true;
    }

    private Response<?> pushTask(Task task) throws IOException {
        if (task.isDeleted()) {
            Response<Task> response = todoApi.deleteTask(task.getId()).execute();
            Log.d(TAG, "DELETE /api/tasks/" + task.getId() + " -> " + response.code());
            return response;
        }

        Response<Task> response = todoApi.updateTask(task.getId(), task).execute();
        Log.d(TAG, "PUT /api/tasks/" + task.getId() + " -> " + response.code());
        if (response.code() == 404) {
            Response<Task> createResponse = todoApi.createTask(task).execute();
            Log.d(TAG, "POST /api/tasks -> " + createResponse.code());
            return createResponse;
        }
        return response;
    }

    /**
     * Lấy dữ liệu Categories từ server về và cập nhật vào local database
     * @return true nếu thành công hoặc không có dữ liệu mới, false nếu cần thử lại sau
     * @throws IOException khi có lỗi kết nối mạng
     */
    private boolean pullRemoteCategories() throws IOException {
        String userId = currentUserId();
        Response<List<Category>> response = todoApi.getAllCategories().execute();
        if (!response.isSuccessful()) {
            if (response.code() == 401) {
                preferenceHelper.clearAuth();
                return false;
            }
            return response.code() < 500;
        }

        List<Category> remoteCategories = response.body();
        List<Category> mapped = new ArrayList<>();
        if (remoteCategories != null) {
            for (Category remote : remoteCategories) {
                Category category = Category.fromRemote(remote);
                category.setUserId(userId);
                Category local = categoryDao.getCategoryById(category.getId(), userId);

                // Local đã xóa nhưng chưa sync -> không lấy bản remote về đè lên
                if (local != null && !local.isSynced() && local.isDeleted()) {
                    continue;
                }
                // Local có thay đổi mới hơn và chưa sync -> giữ bản local
                if (local != null && !local.isSynced() && local.getUpdatedAt() > category.getUpdatedAt()) {
                    continue;
                }
                mapped.add(category);
            }
        }
        if (!mapped.isEmpty()) {
            categoryDao.upsertAll(mapped);
        }
        return true;
    }

    /**
     * Lấy dữ liệu Task từ server về và cập nhật vào local database
     * @return true nếu thành công hoặc không có dữ liệu mới, false nếu cần thử lại sau
     * @throws IOException khi có lỗi kết nối mạng
    */
    private boolean pullRemoteTasks() throws IOException {
        String userId = currentUserId();
        Response<List<Task>> activeResponse = todoApi.getAllTasks().execute();
        if (!activeResponse.isSuccessful()) {
            if (activeResponse.code() == 401) {
                preferenceHelper.clearAuth();
                return false;
            }
            return activeResponse.code() < 500;
        }

       Response<List<Task>> trashResponse = todoApi.getTrashTasks().execute();
        if (!trashResponse.isSuccessful()) {
            if (trashResponse.code() == 401) {
                preferenceHelper.clearAuth();
                return false;
            }
            return trashResponse.code() < 500;
        }

        List<Task> mapped = new ArrayList<>();

        List<Task> activeTasks = activeResponse.body();
        if (activeTasks != null) {
            for (Task remote : activeTasks) {
                Task task = Task.fromRemote(remote);
                task.setUserId(userId);
                task.setDeleted(false);
                addRemoteTaskForUpsert(userId, mapped, task);
            }
        }

        // Xử lý danh sách task trong thùng rác lấy từ server rồi ghi vào Room
        List<Task> trashTasks = trashResponse.body();
        List<String> remoteTrashIds = new ArrayList<>();
        if (trashTasks != null) {
            for (Task remote : trashTasks) {
                Task task = Task.fromRemote(remote);
                task.setUserId(userId);
                task.setDeleted(true);
                remoteTrashIds.add(task.getId());
                addRemoteTaskForUpsert(userId, mapped, task);
            }
        }

        if (!mapped.isEmpty()) {
            taskDao.upsertAll(mapped);
        }

            // Nếu task local đã xóa và chưa sync xóa thì không lấy task từ server xuống nữa
            if (local != null && local.isDeleted()) {
        if (remoteTrashIds.isEmpty()) {
            taskDao.deleteAllSyncedTrash(userId);
        } else {
            taskDao.deleteSyncedTrashNotIn(userId, remoteTrashIds);
        }

        return true;
    }
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

    private void addRemoteTaskForUpsert(String userId, List<Task> mapped, Task task) {
        if (task == null) {
            return;
        }
        Task local = taskDao.getTaskById(task.getId(), userId);

        // Local đã xóa nhưng chưa sync -> không lấy bản remote về đè lên
        if (local != null && !local.isSynced() && local.isDeleted()) {
            return;
        }

        // Local có thay đổi mới hơn và chưa sync -> giữ bản local
        if (local != null && !local.isSynced() && local.getUpdatedAt() > task.getUpdatedAt()) {
            return;
        }

        mapped.add(task);
    }

    private String currentUserId() {
        String userId = preferenceHelper.getAuthUserId();
        return userId == null ? "" : userId;
    }
}
