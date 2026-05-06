package com.example.studentinfoapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivityLifecycle";

    RecyclerView rvTasks, rvCategories;
    Button btnAddTask, btnDeleteSelected;
    ArrayList<Task> taskList;
    TaskAdapter taskAdapter;
    CategoryAdapter categoryAdapter;

    ActivityResultLauncher<Intent> addTaskLauncher;
    ActivityResultLauncher<Intent> detailLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Activity Created");
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        rvTasks = findViewById(R.id.rvTasks);
        rvCategories = findViewById(R.id.rvCategories);
        btnAddTask = findViewById(R.id.btnAddTask);
        btnDeleteSelected = findViewById(R.id.btnDeleteSelected);

        if (savedInstanceState != null) {
            taskList = (ArrayList<Task>) savedInstanceState.getSerializable("taskList");
        }
        if (taskList == null) {
            taskList = new ArrayList<>();
        }

        setupRecyclerViews();
        setupLaunchers();

        btnAddTask.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddTaskActivity.class);
            addTaskLauncher.launch(intent);
        });

        btnDeleteSelected.setOnClickListener(v -> deleteSelectedTasks());
    }

    private void setupRecyclerViews() {
        // Task RecyclerView with DiffUtil & Adapter
        taskAdapter = new TaskAdapter(new TaskAdapter.OnTaskClickListener() {
            @Override
            public void onTaskClick(Task task, int position) {
                if (btnDeleteSelected.getVisibility() == View.VISIBLE) {
                    task.setSelected(!task.isSelected());
                    taskAdapter.notifyItemChanged(position);
                } else {
                    openDetail(task, position);
                }
            }

            @Override
            public void onTaskLongClick(Task task, int position) {
                toggleMultiSelectMode(task, position);
            }

            @Override
            public void onStatusChanged(Task task, boolean isCompleted) {
                task.setCompleted(isCompleted);
            }
        });
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        rvTasks.setAdapter(taskAdapter);
        taskAdapter.submitList(new ArrayList<>(taskList));

        // Swipe-to-delete
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                taskList.remove(position);
                taskAdapter.submitList(new ArrayList<>(taskList));
                Toast.makeText(MainActivity.this, "Task deleted", Toast.LENGTH_SHORT).show();
            }
        }).attachToRecyclerView(rvTasks);

        // Category Horizontal RecyclerView
        List<String> categories = Arrays.asList("All", "Homework", "Project", "Exam");
        categoryAdapter = new CategoryAdapter(categories, category -> {
            Toast.makeText(MainActivity.this, "Filter: " + category, Toast.LENGTH_SHORT).show();
            // TODO: Filter logic could be added here
        });
        rvCategories.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvCategories.setAdapter(categoryAdapter);
    }

    private void toggleMultiSelectMode(Task task, int position) {
        btnDeleteSelected.setVisibility(View.VISIBLE);
        task.setSelected(true);
        taskAdapter.notifyItemChanged(position);
    }

    private void deleteSelectedTasks() {
        List<Task> toRemove = new ArrayList<>();
        for (Task t : taskList) {
            if (t.isSelected()) toRemove.add(t);
        }
        taskList.removeAll(toRemove);
        taskAdapter.submitList(new ArrayList<>(taskList));
        btnDeleteSelected.setVisibility(View.GONE);
        Toast.makeText(this, toRemove.size() + " tasks deleted", Toast.LENGTH_SHORT).show();
    }

    private void openDetail(Task task, int position) {
        Intent intent = new Intent(MainActivity.this, TaskDetailActivity.class);
        intent.putExtra("title", task.getTitle());
        intent.putExtra("description", task.getDescription());
        intent.putExtra("category", task.getCategory());
        intent.putExtra("deadline", task.getDeadline());
        intent.putExtra("completed", task.isCompleted());
        intent.putExtra("priority", task.getPriority());
        intent.putExtra("position", position);
        detailLauncher.launch(intent);
    }

    private void setupLaunchers() {
        addTaskLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Intent data = result.getData();
                Task task = new Task(
                        data.getStringExtra("title"),
                        data.getStringExtra("description"),
                        data.getStringExtra("category"),
                        data.getStringExtra("deadline"),
                        data.getBooleanExtra("completed", false),
                        data.getStringExtra("priority")
                );
                taskList.add(task);
                taskAdapter.submitList(new ArrayList<>(taskList));
            }
        });

        detailLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Intent data = result.getData();
                int delPos = data.getIntExtra("delete_position", -1);
                if (delPos != -1) {
                    taskList.remove(delPos);
                } else if (data.getBooleanExtra("updated", false)) {
                    int pos = data.getIntExtra("position", -1);
                    if (pos != -1) {
                        taskList.set(pos, new Task(
                                data.getStringExtra("title"),
                                data.getStringExtra("description"),
                                data.getStringExtra("category"),
                                data.getStringExtra("deadline"),
                                data.getBooleanExtra("completed", false),
                                data.getStringExtra("priority")
                        ));
                    }
                }
                taskAdapter.submitList(new ArrayList<>(taskList));
            }
        });
    }

//    @Override
//    protected void onStart() { super.onStart(); Log.d(TAG, "onStart"); }
//    @Override
//    protected void onResume() { super.onResume(); Log.d(TAG, "onResume"); }
//    @Override
//    protected void onPause() { super.onPause(); Log.d(TAG, "onPause"); }
//    @Override
//    protected void onStop() { super.onStop(); Log.d(TAG, "onStop"); }
//    @Override
//    protected void onDestroy() { super.onDestroy(); Log.d(TAG, "onDestroy"); }
//    @Override
//    protected void onRestart() { super.onRestart(); Log.d(TAG, "onRestart"); }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("taskList", taskList);
    }
}