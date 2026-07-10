package com.example.taskmanagerapp.data.remote.dto;

public class RefreshTokenRequest {
    private final String refreshToken;
    private final String deviceId;

    public RefreshTokenRequest(String refreshToken, String deviceId) {
        this.refreshToken = refreshToken;
        this.deviceId = deviceId;
    }
}