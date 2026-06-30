package com.example.taskmanagerapp.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.taskmanagerapp.data.model.Category;
import com.example.taskmanagerapp.data.repository.CategoryRepository;

import java.util.List;

public class CategoryViewModel extends AndroidViewModel {
    private final CategoryRepository repository;
    private final LiveData<List<Category>> categories;

    public CategoryViewModel(@NonNull Application application) {
        super(application);
        repository = CategoryRepository.getInstance(application);
        categories = repository.getCategoriesLive();
    }

    public LiveData<List<Category>> getCategories() {
        return categories;
    }

    public void addCategory(String name, String icon) {
        repository.addCategory(name, icon);
    }

    public void renameCategory(Category category, String newName) {
        repository.renameCategory(category, newName);
    }

    public void updateCategory(Category category) {
        repository.updateCategory(category);
    }

    public void togglePinned(Category category) {
        repository.togglePinned(category);
    }

    public void deleteCategory(Category category) {
        repository.deleteCategory(category);
    }

    public void updateCategoryOrders(List<Category> orderedCategories) {
        repository.updateCategoryOrders(orderedCategories);
    }
}
