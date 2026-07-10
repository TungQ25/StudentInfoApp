package com.example.taskmanagerapp.data.remote;

import android.content.Context;

import com.example.taskmanagerapp.data.remote.dto.AuthResponse;
import com.example.taskmanagerapp.data.remote.dto.RefreshTokenRequest;
import com.example.taskmanagerapp.utils.PreferenceHelper;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class RetrofitClient {
    private static final String BASE_URL = "https://taskmanagerapp-zkkh.onrender.com/";

    private static volatile Retrofit retrofit;
    private static volatile Retrofit authRetrofit;
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
                            .connectTimeout(10, TimeUnit.SECONDS)
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
                            .authenticator((route, response) -> refreshAccessToken(response))
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

    private static AuthApi getRefreshAuthApi() {
        if (authRetrofit == null) {
            synchronized (RetrofitClient.class) {
                if (authRetrofit == null) {
                    OkHttpClient client = new OkHttpClient.Builder()
                            .connectTimeout(10, TimeUnit.SECONDS)
                            .readTimeout(30, TimeUnit.SECONDS)
                            .writeTimeout(30, TimeUnit.SECONDS)
                            .build();

                    authRetrofit = new Retrofit.Builder()
                            .baseUrl(BASE_URL)
                            .client(client)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();
                }
            }
        }
        return authRetrofit.create(AuthApi.class);
    }

    private static Request refreshAccessToken(Response response) throws IOException {
        if (appContext == null || responseCount(response) >= 2) {
            return null;
        }

        PreferenceHelper preferences = new PreferenceHelper(appContext);
        String refreshToken = preferences.getRefreshToken();
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            return null;
        }

        synchronized (RetrofitClient.class) {
            String requestToken = bearerToken(response.request());
            String currentToken = preferences.getAuthToken();
            if (currentToken != null && !currentToken.trim().isEmpty() && !currentToken.equals(requestToken)) {
                return response.request().newBuilder()
                        .header("Authorization", "Bearer " + currentToken)
                        .build();
            }

            retrofit2.Response<AuthResponse> refreshResponse = getRefreshAuthApi()
                    .refresh(new RefreshTokenRequest(refreshToken, preferences.getDeviceId()))
                    .execute();
            AuthResponse body = refreshResponse.body();
            if (!refreshResponse.isSuccessful() || body == null || body.getToken() == null || body.getToken().trim().isEmpty()) {
                preferences.clearAuth();
                return null;
            }

            preferences.saveAuth(
                    body.getToken(),
                    body.getId(),
                    body.getUsername(),
                    body.getEmail(),
                    body.getRefreshToken(),
                    body.getRefreshExpiresAt(),
                    body.getDeviceId(),
                    body.getSessionId()
            );

            return response.request().newBuilder()
                    .header("Authorization", "Bearer " + body.getToken())
                    .build();
        }
    }

    private static String bearerToken(Request request) {
        String authorization = request.header("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring("Bearer ".length());
    }

    private static int responseCount(Response response) {
        int count = 1;
        while ((response = response.priorResponse()) != null) {
            count++;
        }
        return count;
    }
}