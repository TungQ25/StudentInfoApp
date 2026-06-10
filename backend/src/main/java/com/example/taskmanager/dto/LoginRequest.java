package com.example.taskmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank
        String identifier,

        @NotBlank
        @Size(min = 6, max = 100)
        String password
) {
}