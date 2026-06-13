package com.example.taskmanagerapp.data.remote.dto;

public class AuthResponse {
    private String id;
    private String username;
    private String email;
    private long createdAt;
    private String token;
    private String tokenType;
    private long expiresAt;

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public String getToken() {
        return token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public long getExpiresAt() {
        return expiresAt;
    }
}
