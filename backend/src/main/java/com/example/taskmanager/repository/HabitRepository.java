package com.example.taskmanager.repository;

import java.util.List;
import java.util.Optional;

import com.example.taskmanager.entity.Habit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HabitRepository extends JpaRepository<Habit, String> {
    @Query("select h from Habit h where h.deleted = false and h.userId = :userId order by h.sortOrder asc, h.createdAt asc")
    List<Habit> findActiveAccessibleHabits(@Param("userId") String userId);

    @Query("select h from Habit h where h.id = :id and h.userId = :userId")
    Optional<Habit> findAccessibleById(@Param("id") String id, @Param("userId") String userId);
}
