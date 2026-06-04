package com.example.taskmanager.controller;

import java.util.List;

import com.example.taskmanager.repository.TaskRepository;
import com.example.taskmanager.entity.Task;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public List<Task> getAllTasks() {
        return repository.findByDeletedFalseOrderByDeadlineAsc();
    }

    @GetMapping("/{id}")
    public Task getTaskById(@PathVariable String id) {
        return repository.findById(id)
                .filter(task -> !task.isDeleted())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
    }

    @PostMapping
    public ResponseEntity<Task> createTask(@Valid @RequestBody Task task) {
        task.setDeleted(false);
        normalizeUpdatedAt(task);
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(task));
    }

    @PutMapping("/{id}")
    public Task updateTask(@PathVariable String id, @Valid @RequestBody Task task) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found");
        }
        task.setId(id);
        task.setDeleted(false);
        normalizeUpdatedAt(task);
        return repository.save(task);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable String id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private static void normalizeUpdatedAt(Task task) {
        if (task.getUpdatedAt() <= 0) {
            task.setUpdatedAt(System.currentTimeMillis());
        }
    }
}
