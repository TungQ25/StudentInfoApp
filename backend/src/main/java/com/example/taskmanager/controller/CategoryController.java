package com.example.taskmanager.controller;

import java.util.List;
import java.util.UUID;

import com.example.taskmanager.dto.CategoryRequest;
import com.example.taskmanager.dto.CategoryResponse;
import com.example.taskmanager.entity.Category;
import com.example.taskmanager.repository.CategoryRepository;
import com.example.taskmanager.repository.TaskRepository;
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
@RequestMapping("/api/categories")
@CrossOrigin(origins = "*")
public class CategoryController {
    private final CategoryRepository categoryRepository;
    private final TaskRepository taskRepository;

    public CategoryController(CategoryRepository categoryRepository, TaskRepository taskRepository) {
        this.categoryRepository = categoryRepository;
        this.taskRepository = taskRepository;
    }

    @GetMapping
    public List<CategoryResponse> getCategories(@AuthenticationPrincipal String userId) {
        return categoryRepository.findVisibleAccessibleCategories(currentUserId(userId))
                .stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(
            @Valid @RequestBody CategoryRequest request,
            @AuthenticationPrincipal String userId) {
        String currentUserId = currentUserId(userId);
        Category category = request.toEntity();
        if (category.getId() == null || category.getId().isBlank()) {
            category.setId(UUID.randomUUID().toString());
        }
        if (categoryRepository.existsById(category.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category id already exists");
        }
        category.setUserId(currentUserId);
        category.setDeleted(false);
        category.setDeletedAt(null);
        category.setVersion(SyncMetadata.initialVersion(request.version()));
        category.setLastModifiedDeviceId(SyncMetadata.normalizeDeviceId(request.deviceId()));
        normalizeTimestamps(category, category);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CategoryResponse.from(categoryRepository.save(category)));
    }

    @PutMapping("/{id}")
    public CategoryResponse updateCategory(
            @PathVariable String id,
            @Valid @RequestBody CategoryRequest request,
            @AuthenticationPrincipal String userId) {
        String currentUserId = currentUserId(userId);
        Category existing = categoryRepository.findAccessibleById(id, currentUserId)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        SyncMetadata.requireFreshVersion(request.version(), existing.getVersion());
        Category requestCategory = request.toEntity();
        request.applyTo(existing);
        normalizeTimestamps(existing, requestCategory);
        existing.setVersion(SyncMetadata.nextVersion(existing.getVersion()));
        existing.setLastModifiedDeviceId(SyncMetadata.normalizeDeviceId(request.deviceId()));
        return CategoryResponse.from(categoryRepository.save(existing));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<CategoryResponse> deleteCategory(
            @PathVariable String id,
            @RequestParam(required = false) String deviceId,
            @AuthenticationPrincipal String userId) {
        String currentUserId = currentUserId(userId);
        Category category = categoryRepository.findAccessibleById(id, currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        long now = System.currentTimeMillis();
        category.setDeleted(true);
        category.setDeletedAt(now);
        category.setPinned(false);
        category.setPinnedOrder(-1);
        category.setUpdatedAt(now);
        category.setVersion(SyncMetadata.nextVersion(category.getVersion()));
        category.setLastModifiedDeviceId(SyncMetadata.normalizeDeviceId(deviceId));
        taskRepository.markCategoryTasksDeleted(id, currentUserId, now, now);
        return ResponseEntity.ok(CategoryResponse.from(categoryRepository.save(category)));
    }

    private static void normalizeTimestamps(Category target, Category request) {
        long now = System.currentTimeMillis();
        if (target.getCreatedAt() <= 0) {
            target.setCreatedAt(request.getCreatedAt() > 0 ? request.getCreatedAt() : now);
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
