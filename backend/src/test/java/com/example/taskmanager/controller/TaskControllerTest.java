package com.example.taskmanager.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.example.taskmanager.dto.TaskRequest;
import com.example.taskmanager.dto.TaskResponse;
import com.example.taskmanager.entity.Task;
import com.example.taskmanager.repository.CategoryRepository;
import com.example.taskmanager.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class TaskControllerTest {
    @Mock
    private TaskRepository taskRepository;

    @Mock
    private CategoryRepository categoryRepository;

    private TaskController controller;

    @BeforeEach
    void setUp() {
        controller = new TaskController(taskRepository, categoryRepository);
    }

    @Test
    void createTaskSetsOwnerVersionAndDeviceMetadata() {
        when(taskRepository.existsById("task-1")).thenReturn(false);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskRequest request = new TaskRequest(
                "task-1",
                "Plan Flutter migration",
                "Draft first milestone",
                null,
                null,
                false,
                false,
                "high",
                null,
                1_700_000_000_000L,
                0L,
                "device-a"
        );

        ResponseEntity<TaskResponse> response = controller.createTask(request, "user-1");

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("user-1", response.getBody().userId());
        assertEquals(1L, response.getBody().version());
        assertEquals("device-a", response.getBody().lastModifiedDeviceId());

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        assertEquals("task-1", captor.getValue().getId());
        assertEquals("user-1", captor.getValue().getUserId());
    }

    @Test
    void updateTaskRejectsStaleVersion() {
        Task existing = new Task();
        existing.setId("task-1");
        existing.setUserId("user-1");
        existing.setTitle("Current title");
        existing.setVersion(3L);

        when(taskRepository.findAccessibleById(eq("task-1"), eq("user-1"))).thenReturn(Optional.of(existing));

        TaskRequest staleRequest = new TaskRequest(
                null,
                "Older title",
                null,
                null,
                null,
                false,
                false,
                null,
                null,
                1_700_000_000_000L,
                2L,
                "device-a"
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.updateTask("task-1", staleRequest, "user-1")
        );
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
    }
}
