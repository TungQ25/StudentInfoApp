package com.example.taskmanagerapp.data.remote;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class RetrofitClient {
    // TODO: Đẩy backend lên online và đổi đường dẫn URL
    private static final String BASE_URL = "http://10.0.2.2:8080/";

    private static volatile Retrofit retrofit;

    // Khai báo để ko cho khởi tạo object kiểu này
    private RetrofitClient() {
    }

    public static Retrofit getInstance() {
        if (retrofit == null) {
            synchronized (RetrofitClient.class) {
                if (retrofit == null) {
                    HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
                    logging.setLevel(HttpLoggingInterceptor.Level.BODY);

                    OkHttpClient client = new OkHttpClient.Builder()
                            .connectTimeout(30, TimeUnit.SECONDS) // sau 30s sẽ timeout
                            .readTimeout(30, TimeUnit.SECONDS)
                            .writeTimeout(30, TimeUnit.SECONDS)
                            .addInterceptor(logging)
                            .build();

                    retrofit = new Retrofit.Builder()
                            .baseUrl(BASE_URL)
                            .client(client) // Dùng OkHttpClient đã cấu hình timeout và logging
                            .addConverterFactory(GsonConverterFactory.create()) // Dùng Gson để tự động chuyển đổi JSON
                            .build(); // Khởi tạo Retrofit instance
                }
            }
        }
        return retrofit;
    }

    /**
     * Lấy Retrofit đã cấu hình sẵn, và tạo ra TodoApi để thực hiện các chức năng (CRUD)
     * @return Đối tượng TodoApi được tạo bởi Retrofit.
     */
    public static TodoApi getTodoApi() {
        return getInstance().create(TodoApi.class);
    }
}
