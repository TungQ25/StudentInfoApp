package com.example.taskmanagerapp.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.taskmanagerapp.data.local.AppDatabase;
import com.example.taskmanagerapp.data.local.HabitCompletionDao;
import com.example.taskmanagerapp.data.local.HabitDao;
import com.example.taskmanagerapp.data.model.Habit;
import com.example.taskmanagerapp.data.model.HabitCompletion;
import com.example.taskmanagerapp.sync.SyncManager;
import com.example.taskmanagerapp.utils.PreferenceHelper;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HabitRepository {
    private static HabitRepository instance;

    private final HabitDao habitDao;
    private final HabitCompletionDao completionDao;
    private final SyncManager syncManager;
    private final PreferenceHelper preferenceHelper;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    private HabitRepository(Context appContext) {
        AppDatabase db = AppDatabase.getInstance(appContext.getApplicationContext());
        this.habitDao = db.habitDao();
        this.completionDao = db.habitCompletionDao();
        this.syncManager = new SyncManager(appContext.getApplicationContext());
        this.preferenceHelper = new PreferenceHelper(appContext.getApplicationContext());
    }

    public static synchronized HabitRepository getInstance(Context context) {
        if (instance == null) {
            instance = new HabitRepository(context.getApplicationContext());
        }
        return instance;
    }

    public LiveData<List<Habit>> getHabitsLive() {
        return habitDao.getHabitsLive(currentUserId());
    }

    public LiveData<List<HabitCompletion>> getCompletionsLive() {
        return completionDao.getCompletionsLive(currentUserId());
    }

    public void addHabit(String title, String groupName, String frequency, int totalDays, int color) {
        ioExecutor.execute(() -> {
            String cleanTitle = title == null ? "" : title.trim();
            if (cleanTitle.isEmpty()) {
                return;
            }
            String userId = currentUserId();
            String cleanGroup = groupName == null || groupName.trim().isEmpty() ? "Habit" : groupName.trim();
            Habit habit = new Habit(cleanTitle, cleanGroup, Habit.normalizeFrequency(frequency), totalDays, color, userId, habitDao.maxSortOrder(userId) + 1);
            habitDao.insert(habit);
            syncManager.syncNow();
        });
    }

    public void toggleCompletion(Habit habit, String periodKey) {
        if (habit == null || periodKey == null || periodKey.trim().isEmpty()) {
            return;
        }
        ioExecutor.execute(() -> {
            String userId = currentUserId();
            Habit localHabit = habitDao.getHabitById(habit.getId(), userId);
            if (localHabit == null) {
                return;
            }

            HabitCompletion completion = completionDao.getByHabitAndPeriod(userId, localHabit.getId(), periodKey);
            if (completion != null && !completion.isDeleted()) {
                completion.markDeletedLocal();
                completionDao.update(completion);
                localHabit.setTotalDays(Math.max(0, localHabit.getTotalDays() - 1));
            } else {
                String completionId = completionId(localHabit.getId(), periodKey);
                if (completion == null) {
                    completion = new HabitCompletion(completionId, localHabit.getId(), userId, periodKey);
                } else {
                    completion.setDeleted(false);
                    completion.setSynced(false);
                    completion.setCompletedAt(System.currentTimeMillis());
                    completion.setUpdatedAt(System.currentTimeMillis());
                }
                completionDao.insert(completion);
                localHabit.setTotalDays(localHabit.getTotalDays() + 1);
            }
            localHabit.markLocalChange();
            habitDao.update(localHabit);
            syncManager.syncNow();
        });
    }

    public void deleteHabit(Habit habit) {
        if (habit == null) {
            return;
        }
        ioExecutor.execute(() -> {
            String userId = currentUserId();
            long now = System.currentTimeMillis();
            habitDao.softDeleteHabit(habit.getId(), userId, now);
            completionDao.softDeleteForHabit(habit.getId(), userId, now);
            syncManager.syncNow();
        });
    }

    public void syncHabits() {
        ioExecutor.execute(syncManager::syncNow);
    }

    private String completionId(String habitId, String periodKey) {
        return habitId + "_" + periodKey.replace(":", "_");
    }

    private String currentUserId() {
        String userId = preferenceHelper.getAuthUserId();
        return userId == null ? "" : userId;
    }
}
