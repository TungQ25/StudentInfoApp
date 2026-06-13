package com.example.taskmanagerapp.data.local;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import androidx.lifecycle.LiveData;

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

    @Query("SELECT * FROM tasks WHERE deleted = 0 AND user_id = :userId ORDER BY deadline ASC")
    List<Task> getAllTasks(String userId);

    @Query("SELECT * FROM tasks WHERE deleted = 0 AND user_id = :userId ORDER BY deadline ASC")
    LiveData<List<Task>> getAllTasksLive(String userId);

    @Query("SELECT * FROM tasks WHERE id = :taskId AND user_id = :userId LIMIT 1")
    Task getTaskById(String taskId, String userId);

    @Query("SELECT * FROM tasks WHERE deleted = 0 AND user_id = :userId AND category = :category ORDER BY deadline ASC")
    List<Task> getTasksByCategory(String userId, String category);

    @Query("SELECT * FROM tasks WHERE deleted = 0 AND user_id = :userId AND is_completed = :completed ORDER BY deadline ASC")
    List<Task> getTasksByCompleted(String userId, boolean completed);

    @Query("SELECT * FROM tasks WHERE deleted = 0 AND user_id = :userId AND title LIKE '%' || :keyword || '%' ORDER BY deadline ASC")
    List<Task> searchByTitle(String userId, String keyword);

    @Query("SELECT * FROM tasks WHERE synced = 0 AND user_id = :userId ORDER BY updated_at ASC")
    List<Task> getPendingSyncTasks(String userId);

    @Query("SELECT * FROM tasks WHERE deleted = 1 AND user_id = :userId ORDER BY updated_at ASC")
    List<Task> getDeletedTasks(String userId);

    @Query("UPDATE tasks SET synced = 1, deleted = 0 WHERE id = :taskId AND user_id = :userId")
    int markSynced(String taskId, String userId);

    @Query("UPDATE tasks SET synced = 1, deleted = 1 WHERE id = :taskId AND user_id = :userId")
    int markDeletedSynced(String taskId, String userId);

    @Query("UPDATE tasks SET deleted = 1, synced = 0, updated_at = :updatedAt WHERE id = :taskId AND user_id = :userId")
    int markDeletedForSync(String taskId, String userId, long updatedAt);

    @Query("DELETE FROM tasks WHERE id = :taskId AND user_id = :userId")
    int deletePermanently(String taskId, String userId);
}
