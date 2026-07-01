package com.example.taskmanagerapp;

import android.app.Application;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.taskmanagerapp.data.local.AppDatabase;
import com.example.taskmanagerapp.data.remote.RetrofitClient;
import com.example.taskmanagerapp.sync.SyncWorker;

import java.util.concurrent.TimeUnit;

public class TaskManagerApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        RetrofitClient.initialize(this);
        AppDatabase.getInstance(this); // khởi tạo database singleton
        schedulePeriodicSync();
    }

    private void schedulePeriodicSync() {
        // Đặt điều kiện chỉ chạy khi có kết nối mạng
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        /* Tạo yêu cầu công việc định kỳ mỗi 15 phút để đồng bộ dữ liệu từ server
        Ràng buộc mạng để chỉ chạy khi có kết nối mạng, 
        Nhận Result.success() nếu đồng bộ thành công, 
        Result.retry() nếu đồng bộ thất bại và tiếp tục retry,
        Result.failure() nếu đồng bộ thất bại và không retry.
        */
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(SyncWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(constraints) // Thiết lập ràng buộc mạng
                .build();

        /* Enqueue work dạng unique tránh tạo nhiều task sync trùng nhau.
        Đăng ký SyncWorker với WorkManager.
        WorkManager sẽ tự gọi doWork() khi đến thời điểm phù hợp và thỏa constraints.
        */
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                SyncWorker.UNIQUE_WORK_NAME, // Tên duy nhất cho công việc định kỳ
                ExistingPeriodicWorkPolicy.UPDATE, // Cập nhật nếu task đã tồn tại tên duy nhất
                request); // Đưa yêu cầu vào hàng đợi của WorkManager
    }
}