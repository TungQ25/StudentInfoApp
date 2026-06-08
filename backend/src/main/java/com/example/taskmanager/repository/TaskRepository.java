package com.example.taskmanager.repository;

import java.util.List;

import com.example.taskmanager.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, String> {
    List<Task> findByDeletedFalseOrderByDeadlineAsc();

    List<Task> findByDeletedTrueOrderByUpdatedAtDesc();

    long deleteByDeletedTrue();

    long deleteByDeletedTrueAndDeletedAtLessThanEqual(long deletedAt);
}
