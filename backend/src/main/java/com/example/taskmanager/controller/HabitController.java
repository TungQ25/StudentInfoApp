package com.example.taskmanager.controller;

import java.util.List;
import java.util.UUID;

import com.example.taskmanager.entity.Habit;
import com.example.taskmanager.repository.HabitCompletionRepository;
import com.example.taskmanager.repository.HabitRepository;
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
    public List<Habit> getHabits(@AuthenticationPrincipal String userId) {
        return habitRepository.findActiveAccessibleHabits(currentUserId(userId));
    }

    @PostMapping
    public ResponseEntity<Habit> createHabit(@Valid @RequestBody Habit habit, @AuthenticationPrincipal String userId) {
        String currentUserId = currentUserId(userId);
        if (habit.getId() == null || habit.getId().isBlank()) {
            habit.setId(UUID.randomUUID().toString());
        }
        Habit existingHabit = habitRepository.findAccessibleById(habit.getId(), currentUserId).orElse(null);
        if (existingHabit != null) {
            if (!existingHabit.isDeleted()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Habit id already exists");
            }
            existingHabit.setTitle(habit.getTitle());
            existingHabit.setGroupName(habit.getGroupName());
            existingHabit.setFrequency(normalizeFrequency(habit.getFrequency()));
            existingHabit.setTotalDays(Math.max(0, habit.getTotalDays()));
            existingHabit.setColor(habit.getColor());
            existingHabit.setSortOrder(habit.getSortOrder());
            existingHabit.setDeleted(false);
            normalizeTimestamps(existingHabit, habit);
            return ResponseEntity.status(HttpStatus.CREATED).body(habitRepository.save(existingHabit));
        }
        if (habitRepository.existsById(habit.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Habit id already exists");
        }
        habit.setUserId(currentUserId);
        habit.setDeleted(false);
        normalizeTimestamps(habit, habit);
        return ResponseEntity.status(HttpStatus.CREATED).body(habitRepository.save(habit));
    }

    @PutMapping("/{id}")
    public Habit updateHabit(@PathVariable String id, @Valid @RequestBody Habit habit, @AuthenticationPrincipal String userId) {
        Habit existing = habitRepository.findAccessibleById(id, currentUserId(userId))
                .filter(h -> !h.isDeleted())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Habit not found"));

        existing.setTitle(habit.getTitle());
        existing.setGroupName(habit.getGroupName());
        existing.setFrequency(normalizeFrequency(habit.getFrequency()));
        existing.setTotalDays(Math.max(0, habit.getTotalDays()));
        existing.setColor(habit.getColor());
        existing.setSortOrder(habit.getSortOrder());
        normalizeTimestamps(existing, habit);
        return habitRepository.save(existing);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Habit> deleteHabit(@PathVariable String id, @AuthenticationPrincipal String userId) {
        String currentUserId = currentUserId(userId);
        Habit habit = habitRepository.findAccessibleById(id, currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Habit not found"));
        long now = System.currentTimeMillis();
        habit.setDeleted(true);
        habit.setUpdatedAt(now);
        completionRepository.markHabitCompletionsDeleted(id, currentUserId, now);
        return ResponseEntity.ok(habitRepository.save(habit));
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
