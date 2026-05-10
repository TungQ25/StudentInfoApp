package com.example.studentinfoapp;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * SQLiteOpenHelper cho bảng tasks. Tăng {@link #DATABASE_VERSION} khi đổi schema và bổ sung bước trong {@link #onUpgrade}.
 */
public class TaskDbHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "student_tasks.db";
    public static final int DATABASE_VERSION = 1;

    public static final String TABLE_TASKS = "tasks";

    public static final String COL_ID = "id";
    public static final String COL_TITLE = "title";
    public static final String COL_DESCRIPTION = "description";
    public static final String COL_CATEGORY = "category";
    public static final String COL_DEADLINE = "deadline";
    public static final String COL_COMPLETED = "is_completed";
    public static final String COL_PRIORITY = "priority";
    public static final String COL_IMAGE_PATH = "image_path";

    private static final String SQL_CREATE_TASKS =
            "CREATE TABLE " + TABLE_TASKS + " ("
                    + COL_ID + " TEXT PRIMARY KEY NOT NULL, "
                    + COL_TITLE + " TEXT NOT NULL, "
                    + COL_DESCRIPTION + " TEXT, "
                    + COL_CATEGORY + " TEXT, "
                    + COL_DEADLINE + " TEXT, "
                    + COL_COMPLETED + " INTEGER NOT NULL DEFAULT 0, "
                    + COL_PRIORITY + " TEXT, "
                    + COL_IMAGE_PATH + " TEXT"
                    + ")";

    public TaskDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Tạo bảng tasks
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TASKS);
    }

    /**
     * Cập nhật bảng tasks
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        int version = oldVersion;
        while (version < newVersion) {
            switch (version) {
                case 1:
                    upgradeFromV1ToV2(db);
                    version = 2;
                    break;
                default:
                    throw new IllegalStateException(
                            "TaskDbHelper: thiếu migration từ DB version " + version);
            }
        }
    }

    /**
     * Ví dụ migration: khi {@link #DATABASE_VERSION} được nâng lên 2, thêm bước tương ứng (ALTER TABLE, bảng mới, v.v.).
     */
    private static void upgradeFromV1ToV2(SQLiteDatabase db) {
        // Ví dụ:
        // db.execSQL("ALTER TABLE " + TABLE_TASKS + " ADD COLUMN some_new_field TEXT");
    }
}
