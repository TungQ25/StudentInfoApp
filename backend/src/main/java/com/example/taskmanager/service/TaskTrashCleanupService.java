package com.example.taskmanager.service;

import java.time.Duration;

import com.example.taskmanager.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskTrashCleanupService {

    private final TaskRepository repository;
    private final long trashRetentionDays;

    public TaskTrashCleanupService(
            TaskRepository repository,
            @Value("${task.trash.retention-days:30}") long trashRetentionDays) {
        this.repository = repository;
        this.trashRetentionDays = trashRetentionDays;
    }

    @Scheduled(fixedDelayString = "${task.trash.cleanup-fixed-delay-ms:86400000}")
    @Transactional
    public void deleteExpiredTrashTasks() {
        long retentionMillis = Duration.ofDays(trashRetentionDays).toMillis();
        long cutoffDeletedAt = System.currentTimeMillis() - retentionMillis; // 30 days ago
        repository.deleteByDeletedTrueAndDeletedAtLessThanEqual(cutoffDeletedAt);
    }
}