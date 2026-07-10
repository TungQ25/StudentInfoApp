package com.example.taskmanager.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class SyncMetadataTest {
    @Test
    void requireFreshVersionRejectsStaleClientVersion() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> SyncMetadata.requireFreshVersion(2L, 3L)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }

    @Test
    void requireFreshVersionAllowsMissingOrCurrentVersion() {
        assertDoesNotThrow(() -> SyncMetadata.requireFreshVersion(0L, 3L));
        assertDoesNotThrow(() -> SyncMetadata.requireFreshVersion(3L, 3L));
    }

    @Test
    void nextVersionStartsAtOneAndThenIncrements() {
        assertEquals(1L, SyncMetadata.nextVersion(0L));
        assertEquals(4L, SyncMetadata.nextVersion(3L));
    }
}
