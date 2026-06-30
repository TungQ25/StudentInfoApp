package com.example.taskmanager.repository;

import java.util.List;
import java.util.Optional;

import com.example.taskmanager.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryRepository extends JpaRepository<Category, String> {
    @Query("select c from Category c where c.deleted = false and c.hidden = false and c.userId = :userId order by c.sortOrder asc, c.name asc")
    List<Category> findVisibleAccessibleCategories(@Param("userId") String userId);

    @Query("select c from Category c where c.id = :id and c.userId = :userId")
    Optional<Category> findAccessibleById(@Param("id") String id, @Param("userId") String userId);
}
