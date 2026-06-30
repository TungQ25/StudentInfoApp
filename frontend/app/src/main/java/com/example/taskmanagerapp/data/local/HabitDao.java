package com.example.taskmanagerapp.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.taskmanagerapp.data.model.Habit;

import java.util.List;

@Dao
public interface HabitDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Habit habit);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertAll(List<Habit> habits);

    @Update
    int update(Habit habit);

    @Query("SELECT * FROM habits WHERE user_id = :userId AND deleted = 0 ORDER BY sort_order ASC, created_at ASC")
    LiveData<List<Habit>> getHabitsLive(String userId);

    @Query("SELECT * FROM habits WHERE user_id = :userId AND deleted = 0 ORDER BY sort_order ASC, created_at ASC")
    List<Habit> getHabits(String userId);

    @Query("SELECT * FROM habits WHERE id = :id AND user_id = :userId LIMIT 1")
    Habit getHabitById(String id, String userId);

    @Query("SELECT COUNT(*) FROM habits WHERE user_id = :userId AND deleted = 0")
    int countHabits(String userId);

    @Query("SELECT COALESCE(MAX(sort_order), -1) FROM habits WHERE user_id = :userId AND deleted = 0")
    int maxSortOrder(String userId);

    @Query("SELECT * FROM habits WHERE synced = 0 AND user_id = :userId ORDER BY updated_at ASC")
    List<Habit> getPendingSyncHabits(String userId);

    @Query("UPDATE habits SET synced = 1, deleted = 0 WHERE id = :habitId AND user_id = :userId")
    int markSynced(String habitId, String userId);

    @Query("UPDATE habits SET synced = 1, deleted = 1 WHERE id = :habitId AND user_id = :userId")
    int markDeletedSynced(String habitId, String userId);

    @Query("UPDATE habits SET deleted = 1, synced = 0, updated_at = :updatedAt WHERE id = :habitId AND user_id = :userId")
    int softDeleteHabit(String habitId, String userId, long updatedAt);
}
