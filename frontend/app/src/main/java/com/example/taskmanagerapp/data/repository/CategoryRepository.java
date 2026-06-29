package com.example.taskmanagerapp.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.taskmanagerapp.data.local.AppDatabase;
import com.example.taskmanagerapp.data.local.CategoryDao;
import com.example.taskmanagerapp.data.local.TaskDao;
import com.example.taskmanagerapp.data.model.Category;
import com.example.taskmanagerapp.sync.SyncManager;
import com.example.taskmanagerapp.utils.PreferenceHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CategoryRepository {
    private static CategoryRepository instance;

    private final CategoryDao categoryDao;
    private final TaskDao taskDao;
    private final SyncManager syncManager;
    private final PreferenceHelper preferenceHelper;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    private CategoryRepository(Context appContext) {
        AppDatabase db = AppDatabase.getInstance(appContext.getApplicationContext());
        this.categoryDao = db.categoryDao();
        this.taskDao = db.taskDao();
        this.syncManager = new SyncManager(appContext.getApplicationContext());
        this.preferenceHelper = new PreferenceHelper(appContext.getApplicationContext());
    }

    public static synchronized CategoryRepository getInstance(Context context) {
        if (instance == null) {
            instance = new CategoryRepository(context.getApplicationContext());
        }
        return instance;
    }

    public LiveData<List<Category>> getCategoriesLive() {
        return categoryDao.getCategoriesLive(currentUserId());
    }

    public void addCategory(String name, String icon) {
        ioExecutor.execute(() -> {
            String trimmedName = name == null ? "" : name.trim();
            if (trimmedName.isEmpty()) {
                return;
            }
            String userId = currentUserId();
            // Kiểm tra category trùng lặp
            if (categoryDao.getByName(userId, trimmedName) != null) {
                return;
            }
            String cleanIcon = icon == null || icon.trim().isEmpty() ? "#" : icon.trim();
            Category category = new Category(trimmedName, cleanIcon, userId, categoryDao.maxSortOrder(userId) + 1);
            categoryDao.insert(category);
            syncManager.syncNow();
        });
    }

    public void renameCategory(Category category, String newName) {
        if (category == null) {
            return;
        }
        ioExecutor.execute(() -> {
            String trimmedName = newName == null ? "" : newName.trim();
            if (trimmedName.isEmpty()) {
                return;
            }
            String userId = currentUserId();
            Category duplicate = categoryDao.getByName(userId, trimmedName);
            if (duplicate != null && !duplicate.getId().equals(category.getId())) {
                return;
            }
            category.setUserId(userId);
            category.setName(trimmedName);
            category.markLocalChange();
            categoryDao.update(category);
            syncManager.syncNow();
        });
    }

    public void updateCategory(Category category) {
        if (category == null) {
            return;
        }
        ioExecutor.execute(() -> {
            category.setUserId(currentUserId());
            category.markLocalChange();
            categoryDao.update(category);
            syncManager.syncNow();
        });
    }

    public void togglePinned(Category category) {
        if (category == null) {
            return;
        }
        ioExecutor.execute(() -> {
            String userId = currentUserId();
            category.setUserId(userId);
            boolean pinned = !category.isPinned();
            category.setPinned(pinned);
            category.setPinnedOrder(pinned ? categoryDao.maxPinnedOrder(userId) + 1 : -1);
            category.markLocalChange();
            categoryDao.update(category);
            syncManager.syncNow();
        });
    }

    public void deleteCategory(Category category) {
        if (category == null) {
            return;
        }
        ioExecutor.execute(() -> {
            String userId = currentUserId();
            long now = System.currentTimeMillis();
            categoryDao.softDeleteCategory(category.getId(), userId, now);
            taskDao.markCategoryTasksDeleted(category.getId(), userId, now);
            syncManager.syncNow();
        });
    }

    public void updateCategoryOrders(List<Category> orderedCategories) {
        if (orderedCategories == null) {
            return;
        }
        ioExecutor.execute(() -> {
            String userId = currentUserId();
            long now = System.currentTimeMillis();
            List<Category> updates = new ArrayList<>();
            for (int i = 0; i < orderedCategories.size(); i++) {
                Category category = orderedCategories.get(i);
                category.setUserId(userId);
                category.setSortOrder(i);
                category.setUpdatedAt(now);
                category.setSynced(false);
                category.setDeleted(false);
                updates.add(category);
            }
            categoryDao.updateAll(updates);
            syncManager.syncNow();
        });
    }

    private String currentUserId() {
        String userId = preferenceHelper.getAuthUserId();
        return userId == null ? "" : userId;
    }
}
