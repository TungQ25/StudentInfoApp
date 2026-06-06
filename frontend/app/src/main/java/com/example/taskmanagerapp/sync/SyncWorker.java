package com.example.taskmanagerapp.sync;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.taskmanagerapp.data.local.AppDatabase;

public class SyncWorker extends Worker {
    public static final String UNIQUE_WORK_NAME = "task_periodic_sync";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    // doWork sẽ chạy khi được WorkManager gọi
    public Result doWork() {
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        try {
            // không tự xử lý toàn bộ sync, mà giao việc cho SyncManager
            SyncManager syncManager = new SyncManager(getApplicationContext(), db.taskDao());
            return syncManager.syncNow() ? Result.success() : Result.retry();
        } catch (Exception e) {
            return Result.failure();
        }
    }
}