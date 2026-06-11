package com.example.taskmanager.repository;

import java.util.List;
import java.util.Optional;

import com.example.taskmanager.entity.Task;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<Task, String> {
    @Query("select t from Task t where t.deleted = false and t.userId = :userId order by t.deadline asc")
    List<Task> findActiveAccessibleTasks(@Param("userId") String userId);

    @Query("select t from Task t where t.deleted = true and t.userId = :userId order by t.updatedAt desc")
    List<Task> findAccessibleTrashTasks(@Param("userId") String userId);

    @Query("select t from Task t where t.id = :id and t.userId = :userId")
    Optional<Task> findAccessibleById(@Param("id") String id, @Param("userId") String userId);

    @Modifying
    @Query("delete from Task t where t.deleted = true and t.userId = :userId")
    int deleteAccessibleTrash(@Param("userId") String userId);

    long deleteByDeletedTrueAndDeletedAtLessThanEqual(long deletedAt);
}
