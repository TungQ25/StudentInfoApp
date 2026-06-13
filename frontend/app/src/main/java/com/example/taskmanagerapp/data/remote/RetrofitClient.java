package com.example.taskmanagerapp.data.remote;

import android.content.Context;
import com.example.taskmanagerapp.utils.PreferenceHelper;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class RetrofitClient {
    private static final String BASE_URL = "http://192.168.1.3:8080/"; // TODO: thay tạm bằng ip máy tính để test, đổi lại khi deploy app

    private static volatile Retrofit retrofit;
    private static volatile Context appContext;

    // Khai báo để ko cho khởi tạo object kiểu này
    private RetrofitClient() {
    }

    public static void initialize(Context context) {
        appContext = context.getApplicationContext();
    }

    public static Retrofit getInstance() {
        if (retrofit == null) {
            synchronized (RetrofitClient.class) {
                if (retrofit == null) {
                    HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
                    logging.setLevel(HttpLoggingInterceptor.Level.BODY);

                    OkHttpClient client = new OkHttpClient.Builder()
                            .connectTimeout(30, TimeUnit.SECONDS)
                            .readTimeout(30, TimeUnit.SECONDS)
                            .writeTimeout(30, TimeUnit.SECONDS)
                            .addInterceptor(chain -> {
                                Request original = chain.request();
                                if (appContext == null) {
                                    return chain.proceed(original);
                                }

                                String token = new PreferenceHelper(appContext).getAuthToken();
                                if (token == null || token.trim().isEmpty()) {
                                    return chain.proceed(original);
                                }

                                Request authenticated = original.newBuilder()
                                        .header("Authorization", "Bearer " + token)
                                        .build();
                                return chain.proceed(authenticated);
                            })
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

    public static AuthApi getAuthApi() {
        return getInstance().create(AuthApi.class);
    }
}
