package com.example.studentinfoapp;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Task task);

    @Update
    int update(Task task);

    @Delete
    int delete(Task task);

    @Query("DELETE FROM tasks WHERE id = :taskId")
    int deleteById(String taskId);

    @Query("SELECT * FROM tasks ORDER BY deadline ASC")
    List<Task> getAllTasks();

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    Task getTaskById(String taskId);

    @Query("SELECT * FROM tasks WHERE category = :category ORDER BY deadline ASC")
    List<Task> getTasksByCategory(String category);

    @Query("SELECT * FROM tasks WHERE is_completed = :completed ORDER BY deadline ASC")
    List<Task> getTasksByCompleted(boolean completed);

    @Query("SELECT * FROM tasks WHERE title LIKE '%' || :keyword || '%' ORDER BY deadline ASC")
    List<Task> searchByTitle(String keyword);
}
