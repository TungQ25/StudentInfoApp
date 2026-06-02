package com.example.studentinfoapp;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Task.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DB_NAME = TaskContract.DATABASE_NAME;
    private static volatile AppDatabase instance;

    public abstract TaskDao taskDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) { // synchronized để tránh race condition khi nhiều thread cùng truy cập vào database
                if (instance == null) {
                    instance = Room.databaseBuilder( // tạo database
                            context.getApplicationContext(),
                                    AppDatabase.class,
                                    DB_NAME) // tên database
                            .build();
                }
            }
        }
        return instance;
    }
}
