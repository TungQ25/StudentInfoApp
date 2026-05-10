package com.example.studentinfoapp;

import android.app.Application;

/**
 * Khởi tạo sớm Room Database khi app start để kiểm tra singleton hoạt động.
 */
public class StudentInfoApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppDatabase.getInstance(this);
    }
}