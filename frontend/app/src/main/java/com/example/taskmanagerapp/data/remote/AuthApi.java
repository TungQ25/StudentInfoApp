package com.example.taskmanagerapp.data.remote;

import com.example.taskmanagerapp.data.remote.dto.AuthResponse;
import com.example.taskmanagerapp.data.remote.dto.LoginRequest;
import com.example.taskmanagerapp.data.remote.dto.LogoutRequest;
import com.example.taskmanagerapp.data.remote.dto.RefreshTokenRequest;
import com.example.taskmanagerapp.data.remote.dto.RegisterRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthApi {
    @POST("api/auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    @POST("api/auth/register")
    Call<AuthResponse> register(@Body RegisterRequest request);

    @POST("api/auth/refresh")
    Call<AuthResponse> refresh(@Body RefreshTokenRequest request);

    @POST("api/auth/logout")
    Call<Void> logout(@Body LogoutRequest request);

    @POST("api/auth/logout-all")
    Call<Void> logoutAll();
}