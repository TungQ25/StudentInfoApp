package com.example.studentinfoapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.DynamicColors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivityLifecycle";

    RecyclerView rvTasks, rvCategories;
    Button btnAddTask, btnDeleteSelected, btnSettings;
    View fragmentContainer, detailScrim;
    TaskAdapter taskAdapter;
    CategoryAdapter categoryAdapter;
    TaskViewModel taskViewModel;
    PreferenceHelper preferenceHelper;
    String appliedTheme;

    ActivityResultLauncher<Intent> addTaskLauncher;
    ActivityResultLauncher<Intent> detailTaskLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applySavedThemeMode();
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
        btnSettings = findViewById(R.id.btnSettings);
        btnDeleteSelected = findViewById(R.id.btnDeleteSelected);
        fragmentContainer = findViewById(R.id.fragment_container);
        detailScrim = findViewById(R.id.detail_scrim); 

        // Initialize ViewModel - đảm bảo dữ liệu được giữ nguyên khi xoay màn hình
        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);

        preferenceHelper = new PreferenceHelper(this);
        appliedTheme = preferenceHelper.getTheme();

        setupRecyclerViews();
        setupLaunchers();

        // Observe tasks from ViewModel - UI tự động cập nhật khi dữ liệu thay đổi (ví dụ: khi thêm, xóa, sửa)
        taskViewModel.getTasks().observe(this, tasks -> {
            taskAdapter.submitList(new ArrayList<>(tasks));
        });

        // Lắng nghe kết quả trả về từ TaskDetailFragment (Fragment Result API)
        getSupportFragmentManager().setFragmentResultListener(TaskDetailFragment.RESULT_KEY, this, (requestKey, bundle) -> {
            String resultMessage = bundle.getString(TaskDetailFragment.RESULT_MESSAGE);
            if (resultMessage != null) {
                Toast.makeText(this, resultMessage, Toast.LENGTH_SHORT).show();
            }
        });
        getSupportFragmentManager().addOnBackStackChangedListener(this::updateDetailOverlayVisibility);

        btnAddTask.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddTaskActivity.class);
            addTaskLauncher.launch(intent);
        });
        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        btnDeleteSelected.setOnClickListener(v -> deleteSelectedTasks());
    }

    private void setupRecyclerViews() {
        taskAdapter = new TaskAdapter(new TaskAdapter.OnTaskClickListener() {
            @Override
            public void onTaskClick(Task task, int position) {
                if (btnDeleteSelected.getVisibility() == View.VISIBLE) {
                    task.setSelected(!task.isSelected());
                    taskAdapter.notifyItemChanged(position);
                } else {
                    showTaskDetail(task);
                }
            }

            @Override
            public void onTaskDoubleClick(Task task, int position) {
                openTaskDetailActivity(task, position);
            }

            @Override
            public void onTaskLongClick(Task task, int position) {
                toggleMultiSelectMode(task, position);
            }

            @Override
            public void onStatusChanged(Task task, boolean isCompleted) {
                task.setCompleted(isCompleted);
                taskViewModel.update(task);
            }
        });
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        rvTasks.setAdapter(taskAdapter);

        // Swipe-to-delete
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Task task = taskAdapter.getCurrentList().get(position);
                taskViewModel.delete(task.getId());
                Toast.makeText(MainActivity.this, "Task deleted", Toast.LENGTH_SHORT).show();
            }
        }).attachToRecyclerView(rvTasks);

        // Category Horizontal RecyclerView
        List<String> categories = Arrays.asList("All", "Homework", "Project", "Exam");
        categoryAdapter = new CategoryAdapter(categories, category -> {
            Toast.makeText(MainActivity.this, "Filter: " + category, Toast.LENGTH_SHORT).show();
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
        List<Task> currentList = taskAdapter.getCurrentList();
        for (Task t : currentList) {
            if (t.isSelected()) {
                taskViewModel.delete(t.getId());
            }
        }
        btnDeleteSelected.setVisibility(View.GONE);
    }

    private void showTaskDetail(Task task) {
        TaskDetailFragment fragment = TaskDetailFragment.newInstance(task);
        fragmentContainer.setVisibility(View.VISIBLE);
        detailScrim.setVisibility(View.VISIBLE);
        getSupportFragmentManager().beginTransaction()
                // Animation khi mở/đóng TaskDetailFragment
//                .setCustomAnimations(
//                        R.anim.slide_in_right,
//                        R.anim.slide_out_left,
//                        R.anim.slide_in_left,
//                        R.anim.slide_out_right
//                )
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void openTaskDetailActivity(Task task, int position) {
        Intent intent = new Intent(MainActivity.this, TaskDetailActivity.class);
        intent.putExtra("id", task.getId());
        intent.putExtra("title", task.getTitle());
        intent.putExtra("description", task.getDescription());
        intent.putExtra("category", task.getCategory());
        intent.putExtra("priority", task.getPriority());
        intent.putExtra("deadline", task.getDeadline());
        intent.putExtra("completed", task.isCompleted());
        intent.putExtra("position", position);
        detailTaskLauncher.launch(intent);
    }

    private void updateDetailOverlayVisibility() {
        boolean hasDetail = getSupportFragmentManager().getBackStackEntryCount() > 0;
        fragmentContainer.setVisibility(hasDetail ? View.VISIBLE : View.GONE);
        detailScrim.setVisibility(hasDetail ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN && getSupportFragmentManager().getBackStackEntryCount() > 0) {
            Rect detailBounds = new Rect();
            fragmentContainer.getGlobalVisibleRect(detailBounds);
            int x = (int) ev.getRawX();
            int y = (int) ev.getRawY();
            if (!detailBounds.contains(x, y)) {
                getSupportFragmentManager().popBackStack();
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String latestTheme = preferenceHelper.getTheme();
        if (!latestTheme.equals(appliedTheme)) {
            appliedTheme = latestTheme;
            applySavedThemeMode();
            recreate();
        }
    }

    private void applySavedThemeMode() {
        SharedPreferences preferences = getSharedPreferences(PreferenceHelper.PREF_NAME, MODE_PRIVATE);
        String theme = preferences.getString(PreferenceHelper.KEY_THEME, PreferenceHelper.THEME_SYSTEM);
        if (PreferenceHelper.THEME_LIGHT.equals(theme)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (PreferenceHelper.THEME_DARK.equals(theme)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
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
                taskViewModel.insert(task);
            }
        });

        detailTaskLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Intent data = result.getData();
                String taskId = data.getStringExtra("id");
                if (taskId == null) {
                    return;
                }

                if (data.getBooleanExtra("deleted", false)) {
                    taskViewModel.delete(taskId);
                    return;
                }

                if (data.getBooleanExtra("updated", false)) {
                    Task updatedTask = new Task(
                            taskId,
                            data.getStringExtra("title"),
                            data.getStringExtra("description"),
                            data.getStringExtra("category"),
                            data.getStringExtra("deadline"),
                            data.getBooleanExtra("completed", false),
                            data.getStringExtra("priority")
                    );
                    taskViewModel.update(updatedTask);
                }
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
}