package com.example.taskmanagerapp.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.taskmanagerapp.data.model.HabitCompletion;

import java.util.List;

@Dao
public interface HabitCompletionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(HabitCompletion completion);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertAll(List<HabitCompletion> completions);

    @Update
    int update(HabitCompletion completion);

    @Query("SELECT * FROM habit_completions WHERE user_id = :userId AND deleted = 0 ORDER BY completed_at DESC")
    LiveData<List<HabitCompletion>> getCompletionsLive(String userId);

    @Query("SELECT * FROM habit_completions WHERE user_id = :userId AND deleted = 0 ORDER BY completed_at DESC")
    List<HabitCompletion> getCompletions(String userId);

    @Query("SELECT * FROM habit_completions WHERE id = :id AND user_id = :userId LIMIT 1")
    HabitCompletion getCompletionById(String id, String userId);

    @Query("SELECT * FROM habit_completions WHERE habit_id = :habitId AND period_key = :periodKey AND user_id = :userId LIMIT 1")
    HabitCompletion getByHabitAndPeriod(String userId, String habitId, String periodKey);

    @Query("SELECT * FROM habit_completions WHERE synced = 0 AND user_id = :userId ORDER BY updated_at ASC")
    List<HabitCompletion> getPendingSyncCompletions(String userId);

    @Query("UPDATE habit_completions SET synced = 1, deleted = 0 WHERE id = :completionId AND user_id = :userId")
    int markSynced(String completionId, String userId);

    @Query("UPDATE habit_completions SET synced = 1, deleted = 1 WHERE id = :completionId AND user_id = :userId")
    int markDeletedSynced(String completionId, String userId);

    @Query("UPDATE habit_completions SET deleted = 1, synced = 0, updated_at = :updatedAt WHERE id = :completionId AND user_id = :userId")
    int softDeleteCompletion(String completionId, String userId, long updatedAt);

    @Query("UPDATE habit_completions SET deleted = 1, synced = 0, updated_at = :updatedAt WHERE habit_id = :habitId AND user_id = :userId")
    int softDeleteForHabit(String habitId, String userId, long updatedAt);
}
