package com.example.taskmanagerapp.data.remote.dto;

public class RegisterRequest {
    private final String username;
    private final String email;
    private final String password;
    private final String deviceId;

    public RegisterRequest(String username, String email, String password) {
        this(username, email, password, null);
    }

    public RegisterRequest(String username, String email, String password, String deviceId) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.deviceId = deviceId;
    }
}