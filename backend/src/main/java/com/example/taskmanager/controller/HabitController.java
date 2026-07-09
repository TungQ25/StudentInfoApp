package com.example.taskmanager.controller;

import java.util.List;
import java.util.UUID;

import com.example.taskmanager.dto.HabitRequest;
import com.example.taskmanager.dto.HabitResponse;
import com.example.taskmanager.entity.Habit;
import com.example.taskmanager.repository.HabitCompletionRepository;
import com.example.taskmanager.repository.HabitRepository;
import com.example.taskmanager.service.SyncMetadata;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
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
@RequestMapping("/api/habits")
@CrossOrigin(origins = "*")
public class HabitController {
    private final HabitRepository habitRepository;
    private final HabitCompletionRepository completionRepository;

    public HabitController(HabitRepository habitRepository, HabitCompletionRepository completionRepository) {
        this.habitRepository = habitRepository;
        this.completionRepository = completionRepository;
    }

    @GetMapping
    public List<HabitResponse> getHabits(@AuthenticationPrincipal String userId) {
        return habitRepository.findActiveAccessibleHabits(currentUserId(userId))
                .stream()
                .map(HabitResponse::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<HabitResponse> createHabit(
            @Valid @RequestBody HabitRequest request,
            @AuthenticationPrincipal String userId) {
        String currentUserId = currentUserId(userId);
        Habit habit = request.toEntity();
        if (habit.getId() == null || habit.getId().isBlank()) {
            habit.setId(UUID.randomUUID().toString());
        }
        Habit existingHabit = habitRepository.findAccessibleById(habit.getId(), currentUserId).orElse(null);
        if (existingHabit != null) {
            if (!existingHabit.isDeleted()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Habit id already exists");
            }
            SyncMetadata.requireFreshVersion(request.version(), existingHabit.getVersion());
            request.applyTo(existingHabit);
            existingHabit.setFrequency(normalizeFrequency(existingHabit.getFrequency()));
            existingHabit.setTotalDays(Math.max(0, existingHabit.getTotalDays()));
            existingHabit.setDeleted(false);
            existingHabit.setDeletedAt(null);
            existingHabit.setVersion(SyncMetadata.nextVersion(existingHabit.getVersion()));
            existingHabit.setLastModifiedDeviceId(SyncMetadata.normalizeDeviceId(request.deviceId()));
            normalizeTimestamps(existingHabit, habit);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(HabitResponse.from(habitRepository.save(existingHabit)));
        }
        if (habitRepository.existsById(habit.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Habit id already exists");
        }
        habit.setUserId(currentUserId);
        habit.setDeleted(false);
        habit.setDeletedAt(null);
        habit.setVersion(SyncMetadata.initialVersion(request.version()));
        habit.setLastModifiedDeviceId(SyncMetadata.normalizeDeviceId(request.deviceId()));
        normalizeTimestamps(habit, habit);
        return ResponseEntity.status(HttpStatus.CREATED).body(HabitResponse.from(habitRepository.save(habit)));
    }

    @PutMapping("/{id}")
    public HabitResponse updateHabit(
            @PathVariable String id,
            @Valid @RequestBody HabitRequest request,
            @AuthenticationPrincipal String userId) {
        Habit existing = habitRepository.findAccessibleById(id, currentUserId(userId))
                .filter(h -> !h.isDeleted())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Habit not found"));

        SyncMetadata.requireFreshVersion(request.version(), existing.getVersion());
        Habit requestHabit = request.toEntity();
        request.applyTo(existing);
        normalizeTimestamps(existing, requestHabit);
        existing.setVersion(SyncMetadata.nextVersion(existing.getVersion()));
        existing.setLastModifiedDeviceId(SyncMetadata.normalizeDeviceId(request.deviceId()));
        return HabitResponse.from(habitRepository.save(existing));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<HabitResponse> deleteHabit(
            @PathVariable String id,
            @RequestParam(required = false) String deviceId,
            @AuthenticationPrincipal String userId) {
        String currentUserId = currentUserId(userId);
        Habit habit = habitRepository.findAccessibleById(id, currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Habit not found"));
        long now = System.currentTimeMillis();
        habit.setDeleted(true);
        habit.setDeletedAt(now);
        habit.setUpdatedAt(now);
        habit.setVersion(SyncMetadata.nextVersion(habit.getVersion()));
        habit.setLastModifiedDeviceId(SyncMetadata.normalizeDeviceId(deviceId));
        completionRepository.markHabitCompletionsDeleted(id, currentUserId, now);
        return ResponseEntity.ok(HabitResponse.from(habitRepository.save(habit)));
    }

    private static void normalizeTimestamps(Habit target, Habit request) {
        long now = System.currentTimeMillis();
        if (target.getCreatedAt() <= 0) {
            target.setCreatedAt(request.getCreatedAt() > 0 ? request.getCreatedAt() : now);
        }
        target.setUpdatedAt(request.getUpdatedAt() > 0 ? request.getUpdatedAt() : now);
        target.setFrequency(normalizeFrequency(target.getFrequency()));
        target.setTotalDays(Math.max(0, target.getTotalDays()));
    }

    private static String normalizeFrequency(String frequency) {
        if ("weekly".equalsIgnoreCase(frequency)) {
            return "weekly";
        }
        if ("monthly".equalsIgnoreCase(frequency)) {
            return "monthly";
        }
        return "daily";
    }

    private static String currentUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return userId;
    }
}
