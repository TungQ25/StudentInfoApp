package com.example.taskmanager.repository;

import java.util.List;
import java.util.Optional;

import com.example.taskmanager.entity.HabitCompletion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HabitCompletionRepository extends JpaRepository<HabitCompletion, String> {
    @Query("select c from HabitCompletion c where c.deleted = false and c.userId = :userId order by c.completedAt desc")
    List<HabitCompletion> findActiveAccessibleCompletions(@Param("userId") String userId);

    @Query("select c from HabitCompletion c where c.id = :id and c.userId = :userId")
    Optional<HabitCompletion> findAccessibleById(@Param("id") String id, @Param("userId") String userId);

    @Modifying
    @Query("update HabitCompletion c set c.deleted = true, c.updatedAt = :updatedAt where c.habitId = :habitId and c.userId = :userId and c.deleted = false")
    int markHabitCompletionsDeleted(@Param("habitId") String habitId, @Param("userId") String userId, @Param("updatedAt") long updatedAt);
}
