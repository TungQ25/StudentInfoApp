package com.example.taskmanagerapp.data.remote;

import com.example.taskmanagerapp.data.model.Category;
import com.example.taskmanagerapp.data.model.Task;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface TodoApi {
    @GET("api/tasks")
    Call<List<Task>> getAllTasks();

    @GET("api/tasks/trash")
    Call<List<Task>> getTrashTasks();

    @GET("api/tasks/{id}")
    Call<Task> getTaskById(@Path("id") String id);

    @POST("api/tasks")
    Call<Task> createTask(@Body Task task);

    @PUT("api/tasks/{id}")
    Call<Task> updateTask(@Path("id") String id, @Body Task task);

    @DELETE("api/tasks/{id}")
    Call<Task> deleteTask(@Path("id") String id);

    @DELETE("api/tasks/{id}/permanent")
    Call<Void> permanentlyDeleteTask(@Path("id") String id);

    @DELETE("api/tasks/trash")
    Call<Void> emptyTrash();

    @GET("api/categories")
    Call<List<Category>> getAllCategories();

    @POST("api/categories")
    Call<Category> createCategory(@Body Category category);

    @PUT("api/categories/{id}")
    Call<Category> updateCategory(@Path("id") String id, @Body Category category);

    @DELETE("api/categories/{id}")
    Call<Category> deleteCategory(@Path("id") String id);
}
