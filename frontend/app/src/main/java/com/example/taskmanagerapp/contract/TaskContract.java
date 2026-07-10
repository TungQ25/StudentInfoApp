package com.example.taskmanagerapp.contract;

public final class TaskContract {

    public static final String DATABASE_NAME = "student_tasks.db";

    public static final String TABLE_TASKS = "tasks";
    public static final String TABLE_CATEGORIES = "categories";
    public static final String TABLE_HABITS = "habits";
    public static final String TABLE_HABIT_COMPLETIONS = "habit_completions";

    public static final String COL_ID = "id";
    public static final String COL_TITLE = "title";
    public static final String COL_DESCRIPTION = "description";
    public static final String COL_CATEGORY_ID = "category_id";
    public static final String COL_DEADLINE = "deadline";
    public static final String COL_COMPLETED = "is_completed";
    public static final String COL_WONT_DO = "wont_do";
    public static final String COL_PRIORITY = "priority";
    public static final String COL_IMAGE_PATH = "image_path";
    public static final String COL_UPDATED_AT = "updated_at";
    public static final String COL_SYNCED = "synced";
    public static final String COL_DELETED = "deleted";
    public static final String COL_PERMANENT_DELETE_PENDING = "permanent_delete_pending";
    public static final String COL_REMOTE_EXISTS = "remote_exists";
    public static final String COL_USER_ID = "user_id";
    public static final String COL_GROUP_NAME = "group_name";
    public static final String COL_FREQUENCY = "frequency";
    public static final String COL_TOTAL_DAYS = "total_days";
    public static final String COL_COLOR = "color";
    public static final String COL_SORT_ORDER = "sort_order";
    public static final String COL_CREATED_AT = "created_at";
    public static final String COL_HABIT_ID = "habit_id";
    public static final String COL_PERIOD_KEY = "period_key";
    public static final String COL_COMPLETED_AT = "completed_at";

    private TaskContract() {
    }
}
