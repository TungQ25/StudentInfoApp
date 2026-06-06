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

    @Query("DELETE FROM tasks WHERE id = :taskId")
    int deleteById(String taskId);

    @Query("SELECT * FROM tasks WHERE deleted = 0 ORDER BY deadline ASC")
    List<Task> getAllTasks();

    @Query("SELECT * FROM tasks WHERE deleted = 0 ORDER BY deadline ASC")
    LiveData<List<Task>> getAllTasksLive();

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    Task getTaskById(String taskId);

    @Query("SELECT * FROM tasks WHERE deleted = 0 AND category = :category ORDER BY deadline ASC")
    List<Task> getTasksByCategory(String category);

    @Query("SELECT * FROM tasks WHERE deleted = 0 AND is_completed = :completed ORDER BY deadline ASC")
    List<Task> getTasksByCompleted(boolean completed);

    @Query("SELECT * FROM tasks WHERE deleted = 0 AND title LIKE '%' || :keyword || '%' ORDER BY deadline ASC")
    List<Task> searchByTitle(String keyword);

    @Query("SELECT * FROM tasks WHERE synced = 0 ORDER BY updated_at ASC")
    List<Task> getPendingSyncTasks();

    @Query("SELECT * FROM tasks WHERE deleted = 1 ORDER BY updated_at ASC")
    List<Task> getDeletedTasks();

    @Query("UPDATE tasks SET synced = 1, deleted = 0 WHERE id = :taskId")
    int markSynced(String taskId);

    @Query("UPDATE tasks SET synced = 1, deleted = 1 WHERE id = :taskId")
    int markDeletedSynced(String taskId);

    @Query("UPDATE tasks SET deleted = 1, synced = 0, updated_at = :updatedAt WHERE id = :taskId")
    int markDeletedForSync(String taskId, long updatedAt);

    @Query("DELETE FROM tasks WHERE id = :taskId")
    int deletePermanently(String taskId);
}
