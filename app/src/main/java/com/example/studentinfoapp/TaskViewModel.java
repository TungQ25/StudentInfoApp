package com.example.studentinfoapp;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

public class TaskViewModel extends AndroidViewModel {
    private final TaskRepository repository;
    private final MutableLiveData<List<Task>> taskListLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public TaskViewModel(@NonNull Application application) {
        super(application);
        this.repository = TaskRepository.getInstance(application);
        refreshTasks();
    }

    public LiveData<List<Task>> getTasks() {
        return taskListLiveData;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public void insert(Task task) {
        isLoading.setValue(true);
        repository.addTask(task);
        refreshTasks();
        isLoading.setValue(false);
    }

    public void update(Task task) {
        isLoading.setValue(true);
        repository.updateTask(task);
        refreshTasks();
        isLoading.setValue(false);
    }

    public void delete(String id) {
        isLoading.setValue(true);
        repository.deleteTask(id);
        refreshTasks();
        isLoading.setValue(false);
    }

    private void refreshTasks() {
        taskListLiveData.setValue(repository.getAllTasks());
    }
}
