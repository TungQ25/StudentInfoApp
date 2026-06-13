package com.example.taskmanagerapp.data.remote;

import com.example.taskmanagerapp.data.remote.dto.AuthResponse;
import com.example.taskmanagerapp.data.remote.dto.LoginRequest;
import com.example.taskmanagerapp.data.remote.dto.RegisterRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthApi {
    @POST("api/auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    @POST("api/auth/register")
    Call<AuthResponse> register(@Body RegisterRequest request);
}