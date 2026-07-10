package com.example.taskmanager.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class SyncMetadata {
    private SyncMetadata() {
    }

    public static void requireFreshVersion(long requestedVersion, long currentVersion) {
        if (requestedVersion > 0 && currentVersion > 0 && requestedVersion < currentVersion) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Version conflict");
        }
    }

    public static long initialVersion(long requestedVersion) {
        return requestedVersion > 0 ? requestedVersion : 1L;
    }

    public static long nextVersion(long currentVersion) {
        return currentVersion > 0 ? currentVersion + 1L : 1L;
    }

    public static String normalizeDeviceId(String deviceId) {
        return deviceId == null || deviceId.isBlank() ? null : deviceId.trim();
    }
}
