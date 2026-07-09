package com.example.taskmanagerapp.data.remote.dto;

public class LogoutRequest {
    private final String refreshToken;

    public LogoutRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}