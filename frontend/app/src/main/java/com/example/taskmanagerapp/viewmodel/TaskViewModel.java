package com.example.taskmanagerapp.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.taskmanagerapp.data.model.Task;
import com.example.taskmanagerapp.data.repository.TaskRepository;

import java.util.List;

public class TaskViewModel extends AndroidViewModel {
    private final TaskRepository repository;
    /** Danh sách task cho UI; đồng bộ từ Room qua {@link TaskRepository#getAllTasksLive()}. */
    private final MutableLiveData<List<Task>> tasks;

    public TaskViewModel(@NonNull Application application) {
        super(application);
        this.repository = TaskRepository.getInstance(application);
        MediatorLiveData<List<Task>> mediator = new MediatorLiveData<>();
        mediator.addSource(repository.getAllTasksLive(), mediator::setValue);
        this.tasks = mediator;
    }

    public LiveData<List<Task>> getTasks() {
        return tasks;
    }

    /**
     * Với Room {@code LiveData}, dữ liệu đã được tải và tự làm mới khi có thay đổi;
     * giữ API này theo yêu cầu bài tập (có thể gọi sau khi observe nếu cần mở rộng sau).
     */
    public void loadTasks() {
        // Không cần thao tác: nguồn repository.getAllTasksLive() đã nối trong constructor.
    }

    public void addTask(Task task) {
        repository.addTask(task);
    }

    public void updateTask(Task task) {
        repository.updateTask(task);
    }

    public void deleteTask(String id) {
        repository.deleteTask(id);
    }

    public void permanentlyDeleteTask(String id) {
        repository.permanentlyDeleteTask(id);
    }

    public void emptyTrash() {
        repository.emptyTrash();
    }

    public void syncTasks() {
        repository.syncTasks();
    }
}
