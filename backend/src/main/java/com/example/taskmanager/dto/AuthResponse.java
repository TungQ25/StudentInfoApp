package com.example.taskmanager.dto;

public record AuthResponse(
        String id,
        String username,
        String email,
        long createdAt,
        String token,
        String tokenType,
        long expiresAt,
        String refreshToken,
        long refreshExpiresAt,
        String deviceId,
        String sessionId
) {
}
