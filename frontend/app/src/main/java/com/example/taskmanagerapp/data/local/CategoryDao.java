package com.example.taskmanagerapp.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.taskmanagerapp.data.model.Category;

import java.util.List;

@Dao
public interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Category category);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertAll(List<Category> categories);

    @Update
    int update(Category category);

    @Update
    void updateAll(List<Category> categories);

    @Query("SELECT * FROM categories WHERE user_id = :userId AND deleted = 0 AND hidden = 0 ORDER BY sortOrder ASC, name ASC")
    LiveData<List<Category>> getCategoriesLive(String userId);

    @Query("SELECT * FROM categories WHERE user_id = :userId AND deleted = 0 AND hidden = 0 ORDER BY sortOrder ASC, name ASC")
    List<Category> getCategories(String userId);

    @Query("SELECT * FROM categories WHERE id = :id AND user_id = :userId LIMIT 1")
    Category getCategoryById(String id, String userId);

    // Lấy category theo tên (không phân biệt hoa thường)
    @Query("SELECT * FROM categories WHERE user_id = :userId AND LOWER(name) = LOWER(:name) AND deleted = 0 LIMIT 1")
    Category getByName(String userId, String name);

    // Lấy category theo thứ tự lớn nhất hiện có
    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM categories WHERE user_id = :userId AND deleted = 0")
    int maxSortOrder(String userId);

    // Lấy category được pin theo thứ tự lớn nhất hiện có
    @Query("SELECT COALESCE(MAX(pinnedOrder), -1) FROM categories WHERE user_id = :userId AND pinned = 1 AND deleted = 0")
    int maxPinnedOrder(String userId);

    // Lấy các categories chưa đồng bộ
    @Query("SELECT * FROM categories WHERE synced = 0 AND user_id = :userId ORDER BY updatedAt ASC")
    List<Category> getPendingSyncCategories(String userId);

    // Đánh dấu đã đồng bộ sau khi upload thành công
    @Query("UPDATE categories SET synced = 1, deleted = 0 WHERE id = :categoryId AND user_id = :userId")
    int markSynced(String categoryId, String userId);

    // Đánh dấu đã đồng bộ cho các mục đã xóa
    @Query("UPDATE categories SET synced = 1, deleted = 1 WHERE id = :categoryId AND user_id = :userId")
    int markDeletedSynced(String categoryId, String userId);

    // Đánh dấu xóa mềm category
    @Query("UPDATE categories SET deleted = 1, synced = 0, updatedAt = :updatedAt WHERE id = :categoryId AND user_id = :userId")
    int softDeleteCategory(String categoryId, String userId, long updatedAt);
}
