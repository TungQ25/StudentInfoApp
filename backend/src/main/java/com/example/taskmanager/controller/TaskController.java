package com.example.taskmanager.controller;

import java.util.List;
import java.util.UUID;

import com.example.taskmanager.entity.Task;
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

    public TaskController(TaskRepository repository) {
        this.repository = repository;
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
        if (task.getId() == null || task.getId().isBlank()) {
            task.setId(UUID.randomUUID().toString());
        }
        if (repository.existsById(task.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Task id already exists");
        }

        task.setDeleted(false);
        task.setDeletedAt(null);
        task.setUserId(currentUserId(userId));
        normalizeOptionalFields(task);
        normalizeUpdatedAt(task, task);
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(task));
    }

    @PutMapping("/{id}")
    public Task updateTask(@PathVariable String id, @Valid @RequestBody Task task, @AuthenticationPrincipal String userId) {
        Task existingTask = repository.findAccessibleById(id, currentUserId(userId))
                .filter(t -> !t.isDeleted())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));

        existingTask.setTitle(task.getTitle());
        existingTask.setDescription(task.getDescription());
        normalizeOptionalFields(task);
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

    /**
     * Kiểm tra updatedAt, giá trị không hợp lý thì gán thời gian hiện tại
     * @param targetTask: task cần cập nhật
     * @param requestTask: task gửi lên 
     */
    private static void normalizeUpdatedAt(Task targetTask, Task requestTask) {
        targetTask.setUpdatedAt(
                requestTask.getUpdatedAt() > 0
                        ? requestTask.getUpdatedAt()
                        : System.currentTimeMillis()
        );
    }

    private static void normalizeOptionalFields(Task task) {
        task.setDescription(blankToNull(task.getDescription()));
        task.setCategoryId(blankToNull(task.getCategoryId()));
        task.setDeadline(blankToNull(task.getDeadline()));
        task.setPriority(blankToNull(task.getPriority()));
        task.setImagePath(blankToNull(task.getImagePath()));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String currentUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return userId;
    }
}
