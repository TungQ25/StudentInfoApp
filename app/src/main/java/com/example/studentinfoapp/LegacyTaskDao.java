package com.example.studentinfoapp;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * CRUD bảng {@link TaskDbHelper#TABLE_TASKS} dùng {@link ContentValues} và {@link Cursor}.
 * Giữ lại để tương thích code cũ trước khi chuyển hoàn toàn sang RoomDatabase.
 */
public class LegacyTaskDao {

    private final TaskDbHelper helper;

    public LegacyTaskDao(@NonNull TaskDbHelper helper) {
        this.helper = helper;
    }

    /** @return row ID của dòng mới, hoặc -1 nếu lỗi */
    public long insertTask(@NonNull Task task) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = taskToContentValues(task);
        return db.insert(TaskDbHelper.TABLE_TASKS, null, values);
    }

    /** @return số dòng bị ảnh hưởng */
    public int updateTask(@NonNull Task task) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues values = taskToContentValues(task);
        return db.update(
                TaskDbHelper.TABLE_TASKS,
                values,
                TaskDbHelper.COL_ID + " = ?",
                new String[]{task.getId()});
    }

    /** @return số dòng đã xóa */
    public int deleteTask(@NonNull String taskId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        return db.delete(
                TaskDbHelper.TABLE_TASKS,
                TaskDbHelper.COL_ID + " = ?",
                new String[]{taskId});
    }

    @NonNull
    public List<Task> getAllTasks() {
        List<Task> out = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.query(
                TaskDbHelper.TABLE_TASKS,
                null,
                null,
                null,
                null,
                null,
                TaskDbHelper.COL_DEADLINE + " ASC")) {
            while (c.moveToNext()) {
                out.add(cursorToTask(c));
            }
        }
        return out;
    }

    @NonNull
    private static ContentValues taskToContentValues(@NonNull Task task) {
        ContentValues v = new ContentValues();
        v.put(TaskDbHelper.COL_ID, task.getId());
        v.put(TaskDbHelper.COL_TITLE, task.getTitle());
        v.put(TaskDbHelper.COL_DESCRIPTION, task.getDescription());
        v.put(TaskDbHelper.COL_CATEGORY, task.getCategory());
        v.put(TaskDbHelper.COL_DEADLINE, task.getDeadline());
        v.put(TaskDbHelper.COL_COMPLETED, task.isCompleted() ? 1 : 0);
        v.put(TaskDbHelper.COL_PRIORITY, task.getPriority());
        v.put(TaskDbHelper.COL_IMAGE_PATH, task.getImagePath());
        return v;
    }

    @NonNull
    private static Task cursorToTask(@NonNull Cursor c) {
        String id = c.getString(c.getColumnIndexOrThrow(TaskDbHelper.COL_ID));
        String title = c.getString(c.getColumnIndexOrThrow(TaskDbHelper.COL_TITLE));
        String description = c.getString(c.getColumnIndexOrThrow(TaskDbHelper.COL_DESCRIPTION));
        String category = c.getString(c.getColumnIndexOrThrow(TaskDbHelper.COL_CATEGORY));
        String deadline = c.getString(c.getColumnIndexOrThrow(TaskDbHelper.COL_DEADLINE));
        boolean completed = c.getInt(c.getColumnIndexOrThrow(TaskDbHelper.COL_COMPLETED)) != 0;
        String priority = c.getString(c.getColumnIndexOrThrow(TaskDbHelper.COL_PRIORITY));

        String imagePath = null;
        int imgIdx = c.getColumnIndex(TaskDbHelper.COL_IMAGE_PATH);
        if (imgIdx >= 0 && !c.isNull(imgIdx)) {
            imagePath = c.getString(imgIdx);
        }
        return new Task(id, title, description, category, deadline, completed, priority, imagePath);
    }
}
