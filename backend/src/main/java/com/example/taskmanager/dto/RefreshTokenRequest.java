package com.example.taskmanager.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank
        String refreshToken,

        String deviceId
) {
}
