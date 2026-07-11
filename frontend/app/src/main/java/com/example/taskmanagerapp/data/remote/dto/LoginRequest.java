package com.example.taskmanagerapp.data.remote.dto;

public class LoginRequest {
    private final String identifier;
    private final String password;
    private final String deviceId;

    public LoginRequest(String identifier, String password) {
        this(identifier, password, null);
    }

    public LoginRequest(String identifier, String password, String deviceId) {
        this.identifier = identifier;
        this.password = password;
        this.deviceId = deviceId;
    }
}