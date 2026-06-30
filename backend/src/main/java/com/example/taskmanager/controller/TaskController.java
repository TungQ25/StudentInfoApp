package com.example.taskmanager.controller;

import java.util.List;
import java.util.UUID;

import com.example.taskmanager.entity.Task;
import com.example.taskmanager.repository.CategoryRepository;
import com.example.taskmanager.repository.TaskRepository;
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
@RequestMapping("/api/tasks")
@CrossOrigin(origins = "*")
public class TaskController {

    private final TaskRepository repository;
    private final CategoryRepository categoryRepository;

    public TaskController(TaskRepository repository, CategoryRepository categoryRepository) {
        this.repository = repository;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public List<Task> getAllTasks(@AuthenticationPrincipal String userId) {
        return repository.findActiveAccessibleTasks(currentUserId(userId));
    }

    @GetMapping("/trash")
    public List<Task> getTrash(@AuthenticationPrincipal String userId) {
        return repository.findAccessibleTrashTasks(currentUserId(userId));
    }

    @GetMapping("/{id}")
    public Task getTaskById(@PathVariable String id, @AuthenticationPrincipal String userId) {
        return repository.findAccessibleById(id, currentUserId(userId))
                .filter(task -> !task.isDeleted())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
    }

    @PostMapping
    public ResponseEntity<Task> createTask(@Valid @RequestBody Task task, @AuthenticationPrincipal String userId) {
        String currentUserId = currentUserId(userId);
        if (task.getId() == null || task.getId().isBlank()) {
            task.setId(UUID.randomUUID().toString());
        }
        if (repository.existsById(task.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Task id already exists");
        }
        validateCategory(task.getCategoryId(), currentUserId);
        task.setDeleted(false);
        task.setDeletedAt(null);
        task.setUserId(currentUserId);
        normalizeUpdatedAt(task, task);
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(task));
    }

    @PutMapping("/{id}")
    public Task updateTask(@PathVariable String id, @Valid @RequestBody Task task, @AuthenticationPrincipal String userId) {
        String currentUserId = currentUserId(userId);
        Task existingTask = repository.findAccessibleById(id, currentUserId)
                .filter(t -> !t.isDeleted())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));

        validateCategory(task.getCategoryId(), currentUserId);
        existingTask.setTitle(task.getTitle());
        existingTask.setDescription(task.getDescription());
        existingTask.setCategoryId(task.getCategoryId());
        existingTask.setDeadline(task.getDeadline());
        existingTask.setCompleted(task.isCompleted());
        existingTask.setWontDo(task.isWontDo());
        existingTask.setPriority(task.getPriority());
        existingTask.setImagePath(task.getImagePath());
        normalizeUpdatedAt(existingTask, task);

        return repository.save(existingTask);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Task> softDeleteTask(@PathVariable String id, @AuthenticationPrincipal String userId) {
        return repository.findAccessibleById(id, currentUserId(userId))
                .map(task -> {
                    task.setDeleted(true);
                    task.setDeletedAt(System.currentTimeMillis());
                    task.setUpdatedAt(System.currentTimeMillis());
                    return ResponseEntity.ok(repository.save(task));
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<Task> restoreTask(@PathVariable String id, @AuthenticationPrincipal String userId) {
        return repository.findAccessibleById(id, currentUserId(userId))
                .map(task -> {
                    task.setDeleted(false);
                    task.setDeletedAt(null);
                    task.setUpdatedAt(System.currentTimeMillis());
                    return ResponseEntity.ok(repository.save(task));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<Void> permanentlyDeleteTask(@PathVariable String id, @AuthenticationPrincipal String userId) {
        return repository.findAccessibleById(id, currentUserId(userId))
                .map(task -> {
                    if (!task.isDeleted()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Task must be in trash to be permanently deleted");
                    }
                    repository.deleteById(id);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found in trash"));
    }

    @DeleteMapping("/trash")
    @Transactional
    public ResponseEntity<Void> emptyTrash(@AuthenticationPrincipal String userId) {
        repository.deleteAccessibleTrash(currentUserId(userId));
        return ResponseEntity.noContent().build();
    }

    private void validateCategory(String categoryId, String userId) {
        // Task không có category thì bỏ qua kiểm tra
        if (categoryId == null || categoryId.isBlank()) {
            return;
        }
        // Chỉ duyệt category chưa bị xoá mềm, không thì báo lỗi
        categoryRepository.findAccessibleById(categoryId, userId)
                .filter(category -> !category.isDeleted())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category not found"));
    }

    private static void normalizeUpdatedAt(Task targetTask, Task requestTask) {
        targetTask.setUpdatedAt(
                requestTask.getUpdatedAt() > 0
                        ? requestTask.getUpdatedAt()
                        : System.currentTimeMillis()
        );
    }

    private static String currentUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return userId;
    }
}
