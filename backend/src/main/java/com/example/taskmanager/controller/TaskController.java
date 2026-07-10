package com.example.taskmanager.controller;

import java.util.List;
import java.util.UUID;

import com.example.taskmanager.dto.TaskRequest;
import com.example.taskmanager.dto.TaskResponse;
import com.example.taskmanager.entity.Task;
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
    public List<TaskResponse> getAllTasks(@AuthenticationPrincipal String userId) {
        return repository.findActiveAccessibleTasks(currentUserId(userId))
                .stream()
                .map(TaskResponse::from)
                .toList();
    }

    @GetMapping("/trash")
    public List<TaskResponse> getTrash(@AuthenticationPrincipal String userId) {
        return repository.findAccessibleTrashTasks(currentUserId(userId))
                .stream()
                .map(TaskResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public TaskResponse getTaskById(@PathVariable String id, @AuthenticationPrincipal String userId) {
        return repository.findAccessibleById(id, currentUserId(userId))
                .filter(task -> !task.isDeleted())
                .map(TaskResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
    }

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody TaskRequest request, @AuthenticationPrincipal String userId) {
        String currentUserId = currentUserId(userId);
        Task task = request.toEntity();
        if (task.getId() == null || task.getId().isBlank()) {
            task.setId(UUID.randomUUID().toString());
        }
        if (repository.existsById(task.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Task id already exists");
        }
        normalizeOptionalFields(task);
        validateCategory(task.getCategoryId(), currentUserId);
        task.setDeleted(false);
        task.setDeletedAt(null);
        task.setUserId(currentUserId);
        task.setVersion(SyncMetadata.initialVersion(request.version()));
        task.setLastModifiedDeviceId(SyncMetadata.normalizeDeviceId(request.deviceId()));
        normalizeUpdatedAt(task, task);
        return ResponseEntity.status(HttpStatus.CREATED).body(TaskResponse.from(repository.save(task)));
    }

    @PutMapping("/{id}")
    public TaskResponse updateTask(@PathVariable String id, @Valid @RequestBody TaskRequest request, @AuthenticationPrincipal String userId) {
        String currentUserId = currentUserId(userId);
        Task existingTask = repository.findAccessibleById(id, currentUserId)
                .filter(t -> !t.isDeleted())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));

        SyncMetadata.requireFreshVersion(request.version(), existingTask.getVersion());
        Task requestTask = request.toEntity();
        normalizeOptionalFields(requestTask);
        validateCategory(requestTask.getCategoryId(), currentUserId);
        request.applyTo(existingTask);
        normalizeOptionalFields(existingTask);
        normalizeUpdatedAt(existingTask, requestTask);
        existingTask.setVersion(SyncMetadata.nextVersion(existingTask.getVersion()));
        existingTask.setLastModifiedDeviceId(SyncMetadata.normalizeDeviceId(request.deviceId()));

        return TaskResponse.from(repository.save(existingTask));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<TaskResponse> softDeleteTask(
            @PathVariable String id,
            @RequestParam(required = false) String deviceId,
            @AuthenticationPrincipal String userId) {
        return repository.findAccessibleById(id, currentUserId(userId))
                .map(task -> {
                    task.setDeleted(true);
                    task.setDeletedAt(System.currentTimeMillis());
                    task.setUpdatedAt(System.currentTimeMillis());
                    task.setVersion(SyncMetadata.nextVersion(task.getVersion()));
                    task.setLastModifiedDeviceId(SyncMetadata.normalizeDeviceId(deviceId));
                    return ResponseEntity.ok(TaskResponse.from(repository.save(task)));
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<TaskResponse> restoreTask(
            @PathVariable String id,
            @RequestParam(required = false) String deviceId,
            @AuthenticationPrincipal String userId) {
        return repository.findAccessibleById(id, currentUserId(userId))
                .map(task -> {
                    task.setDeleted(false);
                    task.setDeletedAt(null);
                    task.setUpdatedAt(System.currentTimeMillis());
                    task.setVersion(SyncMetadata.nextVersion(task.getVersion()));
                    task.setLastModifiedDeviceId(SyncMetadata.normalizeDeviceId(deviceId));
                    return ResponseEntity.ok(TaskResponse.from(repository.save(task)));
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
