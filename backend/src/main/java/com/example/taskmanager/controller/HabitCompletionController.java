package com.example.taskmanager.controller;

import java.util.List;
import java.util.UUID;

import com.example.taskmanager.dto.HabitCompletionRequest;
import com.example.taskmanager.dto.HabitCompletionResponse;
import com.example.taskmanager.entity.HabitCompletion;
import com.example.taskmanager.repository.HabitCompletionRepository;
import com.example.taskmanager.repository.HabitRepository;
import com.example.taskmanager.service.SyncMetadata;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/habit-completions")
@CrossOrigin(origins = "*")
public class HabitCompletionController {
    private final HabitCompletionRepository completionRepository;
    private final HabitRepository habitRepository;

    public HabitCompletionController(HabitCompletionRepository completionRepository, HabitRepository habitRepository) {
        this.completionRepository = completionRepository;
        this.habitRepository = habitRepository;
    }

    @GetMapping
    public List<HabitCompletionResponse> getCompletions(@AuthenticationPrincipal String userId) {
        return completionRepository.findActiveAccessibleCompletions(currentUserId(userId))
                .stream()
                .map(HabitCompletionResponse::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<HabitCompletionResponse> createCompletion(
            @Valid @RequestBody HabitCompletionRequest request,
            @AuthenticationPrincipal String userId) {
        String currentUserId = currentUserId(userId);
        HabitCompletion completion = request.toEntity();
        if (completion.getId() == null || completion.getId().isBlank()) {
            completion.setId(UUID.randomUUID().toString());
        }
        HabitCompletion existingCompletion = completionRepository.findAccessibleById(completion.getId(), currentUserId).orElse(null);
        if (existingCompletion != null) {
            if (!existingCompletion.isDeleted()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Habit completion id already exists");
            }
            SyncMetadata.requireFreshVersion(request.version(), existingCompletion.getVersion());
            validateHabit(completion.getHabitId(), currentUserId);
            request.applyTo(existingCompletion);
            existingCompletion.setDeleted(false);
            existingCompletion.setDeletedAt(null);
            existingCompletion.setVersion(SyncMetadata.nextVersion(existingCompletion.getVersion()));
            existingCompletion.setLastModifiedDeviceId(SyncMetadata.normalizeDeviceId(request.deviceId()));
            normalizeTimestamps(existingCompletion, completion);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(HabitCompletionResponse.from(completionRepository.save(existingCompletion)));
        }
        if (completionRepository.existsById(completion.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Habit completion id already exists");
        }
        validateHabit(completion.getHabitId(), currentUserId);
        completion.setUserId(currentUserId);
        completion.setDeleted(false);
        completion.setDeletedAt(null);
        completion.setVersion(SyncMetadata.initialVersion(request.version()));
        completion.setLastModifiedDeviceId(SyncMetadata.normalizeDeviceId(request.deviceId()));
        normalizeTimestamps(completion, completion);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(HabitCompletionResponse.from(completionRepository.save(completion)));
    }

    @PutMapping("/{id}")
    public HabitCompletionResponse updateCompletion(
            @PathVariable String id,
            @Valid @RequestBody HabitCompletionRequest request,
            @AuthenticationPrincipal String userId) {
        String currentUserId = currentUserId(userId);
        HabitCompletion existing = completionRepository.findAccessibleById(id, currentUserId)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Habit completion not found"));

        SyncMetadata.requireFreshVersion(request.version(), existing.getVersion());
        HabitCompletion requestCompletion = request.toEntity();
        validateHabit(requestCompletion.getHabitId(), currentUserId);
        request.applyTo(existing);
        normalizeTimestamps(existing, requestCompletion);
        existing.setVersion(SyncMetadata.nextVersion(existing.getVersion()));
        existing.setLastModifiedDeviceId(SyncMetadata.normalizeDeviceId(request.deviceId()));
        return HabitCompletionResponse.from(completionRepository.save(existing));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<HabitCompletionResponse> deleteCompletion(
            @PathVariable String id,
            @RequestParam(required = false) String deviceId,
            @AuthenticationPrincipal String userId) {
        HabitCompletion completion = completionRepository.findAccessibleById(id, currentUserId(userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Habit completion not found"));
        long now = System.currentTimeMillis();
        completion.setDeleted(true);
        completion.setDeletedAt(now);
        completion.setUpdatedAt(now);
        completion.setVersion(SyncMetadata.nextVersion(completion.getVersion()));
        completion.setLastModifiedDeviceId(SyncMetadata.normalizeDeviceId(deviceId));
        return ResponseEntity.ok(HabitCompletionResponse.from(completionRepository.save(completion)));
    }

    private void validateHabit(String habitId, String userId) {
        if (habitId == null || habitId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Habit id is required");
        }
        habitRepository.findAccessibleById(habitId, userId)
                .filter(habit -> !habit.isDeleted())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Habit not found"));
    }

    private static void normalizeTimestamps(HabitCompletion target, HabitCompletion request) {
        long now = System.currentTimeMillis();
        if (target.getCompletedAt() <= 0) {
            target.setCompletedAt(request.getCompletedAt() > 0 ? request.getCompletedAt() : now);
        }
        target.setUpdatedAt(request.getUpdatedAt() > 0 ? request.getUpdatedAt() : now);
    }

    private static String currentUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return userId;
    }
}
