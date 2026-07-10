package com.example.taskmanagerapp.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.taskmanagerapp.data.model.Task;

import java.util.List;

@Dao
public interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Task task);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertAll(List<Task> tasks);

    @Update
    int update(Task task);

    @Delete
    int delete(Task task);

    @Query("DELETE FROM tasks WHERE id = :taskId AND user_id = :userId")
    int deleteById(String taskId, String userId);

    @Query("SELECT * FROM tasks WHERE deleted = 0 AND permanent_delete_pending = 0 AND user_id = :userId ORDER BY deadline ASC")
    List<Task> getAllTasks(String userId);

    @Query("SELECT * FROM tasks WHERE user_id = :userId AND permanent_delete_pending = 0 ORDER BY deleted ASC, deadline ASC")
    LiveData<List<Task>> getAllTasksLive(String userId);

    @Query("SELECT * FROM tasks WHERE id = :taskId AND user_id = :userId LIMIT 1")
    Task getTaskById(String taskId, String userId);

    @Query("SELECT * FROM tasks WHERE deleted = 0 AND permanent_delete_pending = 0 AND user_id = :userId AND category_id = :categoryId ORDER BY deadline ASC")
    List<Task> getTasksByCategory(String userId, String categoryId);

    @Query("SELECT * FROM tasks WHERE deleted = 0 AND permanent_delete_pending = 0 AND user_id = :userId AND is_completed = :completed ORDER BY deadline ASC")
    List<Task> getTasksByCompleted(String userId, boolean completed);

    @Query("SELECT * FROM tasks WHERE deleted = 0 AND permanent_delete_pending = 0 AND user_id = :userId AND title LIKE '%' || :keyword || '%' ORDER BY deadline ASC")
    List<Task> searchByTitle(String userId, String keyword);

    @Query("SELECT * FROM tasks WHERE synced = 0 AND permanent_delete_pending = 0 AND user_id = :userId ORDER BY updated_at ASC")
    List<Task> getPendingSyncTasks(String userId);

    @Query("SELECT * FROM tasks WHERE permanent_delete_pending = 1 AND user_id = :userId ORDER BY updated_at ASC")
    List<Task> getPendingPermanentDeleteTasks(String userId);

    @Query("SELECT * FROM tasks WHERE deleted = 1 AND permanent_delete_pending = 0 AND user_id = :userId ORDER BY updated_at ASC")
    List<Task> getDeletedTasks(String userId);

    @Query("UPDATE tasks SET synced = 1, deleted = 0, permanent_delete_pending = 0, remote_exists = 1 WHERE id = :taskId AND user_id = :userId")
    int markSynced(String taskId, String userId);

    @Query("UPDATE tasks SET synced = 1, deleted = 1, permanent_delete_pending = 0, remote_exists = 1 WHERE id = :taskId AND user_id = :userId")
    int markDeletedSynced(String taskId, String userId);

    @Query("UPDATE tasks SET deleted = 1, synced = 0, permanent_delete_pending = 0, updated_at = :updatedAt WHERE id = :taskId AND user_id = :userId")
    int markDeletedForSync(String taskId, String userId, long updatedAt);

    @Query("UPDATE tasks SET deleted = 1, synced = 0, updated_at = :updatedAt WHERE category_id = :categoryId AND user_id = :userId AND deleted = 0")
    int markCategoryTasksDeleted(String categoryId, String userId, long updatedAt);

    @Query("UPDATE tasks SET deleted = 1, synced = 0, permanent_delete_pending = 1, updated_at = :updatedAt WHERE id = :taskId AND user_id = :userId")
    int markPermanentDeletePending(String taskId, String userId, long updatedAt);

    @Query("UPDATE tasks SET deleted = 1, synced = 0, permanent_delete_pending = 1, updated_at = :updatedAt WHERE deleted = 1 AND user_id = :userId")
    int markAllTrashPermanentDeletePending(String userId, long updatedAt);

    @Query("DELETE FROM tasks WHERE id = :taskId AND user_id = :userId")
    int deletePermanently(String taskId, String userId);

    @Query("DELETE FROM tasks WHERE deleted = 1 AND synced = 1 AND user_id = :userId")
    int deleteAllSyncedTrash(String userId);

    @Query("DELETE FROM tasks WHERE deleted = 1 AND synced = 1 AND user_id = :userId AND id NOT IN (:taskIds)")
    int deleteSyncedTrashNotIn(String userId, List<String> taskIds);

    @Query("DELETE FROM tasks WHERE remote_exists = 1 AND synced = 1 AND permanent_delete_pending = 0 AND user_id = :userId")
    int deleteAllRemoteSyncedTasks(String userId);

    @Query("DELETE FROM tasks WHERE remote_exists = 1 AND synced = 1 AND permanent_delete_pending = 0 AND user_id = :userId AND id NOT IN (:taskIds)")
    int deleteRemoteSyncedTasksNotIn(String userId, List<String> taskIds);

    @Query("DELETE FROM tasks WHERE deleted = 1 AND user_id = :userId")
    int deleteAllTrash(String userId);
}
