package com.example.taskmanagerapp.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import android.app.Activity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.taskmanagerapp.R;
import com.example.taskmanagerapp.data.model.Task;
import com.example.taskmanagerapp.ui.activity.AddTaskActivity;
import com.example.taskmanagerapp.ui.activity.TaskDetailActivity;
import com.example.taskmanagerapp.ui.adapter.CategoryAdapter;
import com.example.taskmanagerapp.ui.adapter.TaskAdapter;
import com.example.taskmanagerapp.utils.ImageStorageHelper;
import com.example.taskmanagerapp.viewmodel.TaskViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TaskFragment extends Fragment implements TaskDetailFragment.OnNavigateToFullDetailListener {
    private RecyclerView rvTasks;
    private RecyclerView rvCategories;
    private FloatingActionButton btnAddTask;
    private Button btnDeleteSelected;
    private View fragmentContainer;
    private View detailScrim;
    private TaskAdapter taskAdapter;
    private TaskViewModel taskViewModel;
    private ImageStorageHelper imageStorage;
    private ActivityResultLauncher<Intent> addTaskLauncher;
    private ActivityResultLauncher<Intent> detailTaskLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addTaskLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Intent data = result.getData();
                Task task = new Task(
                        data.getStringExtra("title"),
                        data.getStringExtra("description"),
                        data.getStringExtra("category"),
                        data.getStringExtra("deadline"),
                        data.getBooleanExtra("completed", false),
                        data.getStringExtra("priority")
                );
                task.setImagePath(data.getStringExtra(AddTaskActivity.EXTRA_IMAGE_PATH));
                taskViewModel.addTask(task);
                taskViewModel.loadTasks();
            }
        });

        detailTaskLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                handleTaskDetailResult(result.getData());
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_task, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rvTasks = view.findViewById(R.id.rvTasks);
        rvCategories = view.findViewById(R.id.rvCategories);
        btnAddTask = view.findViewById(R.id.btnAddTask);
        btnDeleteSelected = view.findViewById(R.id.btnDeleteSelected);
        fragmentContainer = view.findViewById(R.id.fragment_container);
        detailScrim = view.findViewById(R.id.detail_scrim);
        taskViewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);
        imageStorage = new ImageStorageHelper(requireContext());

        setupRecyclerViews();
        setupDetailOverlay();
        setupActions();
        observeTasks();
        taskViewModel.syncTasks();
    }

    private void setupRecyclerViews() {
        taskAdapter = new TaskAdapter(new TaskAdapter.OnTaskClickListener() {
            @Override
            public void onTaskClick(Task task, int position) {
                if (btnDeleteSelected != null && btnDeleteSelected.getVisibility() == View.VISIBLE) {
                    task.setSelected(!task.isSelected());
                    taskAdapter.notifyItemChanged(position);
                } else {
                    showTaskDetail(task);
                }
            }

            @Override
            public void onTaskLongClick(Task task, int position) {
                toggleMultiSelectMode(task, position);
            }

            @Override
            public void onStatusChanged(Task task, boolean isCompleted) {
                task.setCompleted(isCompleted);
                taskViewModel.updateTask(task);
            }
        });
        rvTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTasks.setAdapter(taskAdapter);

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Task task = taskAdapter.getCurrentList().get(position);
                deleteTaskAndImage(task);
                Toast.makeText(requireContext(), "Task deleted", Toast.LENGTH_SHORT).show();
            }
        }).attachToRecyclerView(rvTasks);

        List<String> categories = Arrays.asList("All", "Homework", "Project", "Exam");
        CategoryAdapter categoryAdapter = new CategoryAdapter(categories, category ->
                Toast.makeText(requireContext(), "Filter: " + category, Toast.LENGTH_SHORT).show()
        );
        rvCategories.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvCategories.setAdapter(categoryAdapter);
    }

    private void setupDetailOverlay() {
        getChildFragmentManager().setFragmentResultListener(TaskDetailFragment.RESULT_KEY, getViewLifecycleOwner(), (requestKey, bundle) -> {
            String resultMessage = bundle.getString(TaskDetailFragment.RESULT_MESSAGE);
            if (resultMessage != null) {
                Toast.makeText(requireContext(), resultMessage, Toast.LENGTH_SHORT).show();
            }
        });
        getChildFragmentManager().addOnBackStackChangedListener(this::updateDetailOverlayVisibility);
        if (detailScrim != null) {
            detailScrim.setOnClickListener(v -> getChildFragmentManager().popBackStack());
        }
    }

    private void setupActions() {
        btnAddTask.setOnClickListener(v -> addTaskLauncher.launch(new Intent(requireContext(), AddTaskActivity.class)));
        btnDeleteSelected.setOnClickListener(v -> deleteSelectedTasks());
    }

    private void observeTasks() {
        taskViewModel.getTasks().observe(getViewLifecycleOwner(), tasks -> {
            if (tasks != null) {
                taskAdapter.submitList(new ArrayList<>(tasks));
            }
        });
    }

    private void toggleMultiSelectMode(Task task, int position) {
        btnDeleteSelected.setVisibility(View.VISIBLE);
        task.setSelected(true);
        taskAdapter.notifyItemChanged(position);
    }

    private void deleteSelectedTasks() {
        List<Task> currentList = taskAdapter.getCurrentList();
        for (Task task : currentList) {
            if (task.isSelected()) {
                deleteTaskAndImage(task);
            }
        }
        btnDeleteSelected.setVisibility(View.GONE);
    }

    private void deleteTaskAndImage(Task task) {
        if (task == null) return;
        String img = task.getImagePath();
        if (img != null && !img.isEmpty()) {
            imageStorage.deleteImage(img);
        }
        taskViewModel.deleteTask(task.getId());
    }

    private Task findTaskById(String taskId) {
        if (taskId == null) return null;
        for (Task task : taskAdapter.getCurrentList()) {
            if (taskId.equals(task.getId())) return task;
        }
        return null;
    }

    private boolean isTwoPane() {
        return getResources().getConfiguration().smallestScreenWidthDp >= 600;
    }

    private void showTaskDetail(Task task) {
        TaskDetailFragment fragment = TaskDetailFragment.newInstance(task);
        if (fragmentContainer != null) fragmentContainer.setVisibility(View.VISIBLE);
        if (detailScrim != null) detailScrim.setVisibility(isTwoPane() ? View.GONE : View.VISIBLE);
        getChildFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onNavigateToFullDetail(Task task) {
        if (task == null) {
            return;
        }
        getChildFragmentManager().popBackStack();
        int position = findTaskListPosition(task);
        openTaskDetailActivity(task, position >= 0 ? position : 0);
    }

    private int findTaskListPosition(Task task) {
        if (task == null || task.getId() == null) {
            return -1;
        }
        List<Task> list = taskAdapter.getCurrentList();
        for (int i = 0; i < list.size(); i++) {
            if (task.getId().equals(list.get(i).getId())) {
                return i;
            }
        }
        return -1;
    }

    private void openTaskDetailActivity(Task task, int position) {
        Intent intent = new Intent(requireContext(), TaskDetailActivity.class);
        intent.putExtra("id", task.getId());
        intent.putExtra("title", task.getTitle());
        intent.putExtra("description", task.getDescription());
        intent.putExtra("category", task.getCategory());
        intent.putExtra("priority", task.getPriority());
        intent.putExtra("deadline", task.getDeadline());
        intent.putExtra("completed", task.isCompleted());
        intent.putExtra("position", position);
        intent.putExtra(AddTaskActivity.EXTRA_IMAGE_PATH, task.getImagePath());
        detailTaskLauncher.launch(intent);
    }

    private void handleTaskDetailResult(Intent data) {
        String taskId = data.getStringExtra("id");
        if (taskId == null) {
            return;
        }

        if (data.getBooleanExtra("deleted", false)) {
            Task existing = findTaskById(taskId);
            if (existing != null) {
                deleteTaskAndImage(existing);
            } else {
                String img = data.getStringExtra(AddTaskActivity.EXTRA_IMAGE_PATH);
                if (img != null) imageStorage.deleteImage(img);
                taskViewModel.deleteTask(taskId);
            }
            return;
        }

        if (data.getBooleanExtra("updated", false)) {
            String newImagePath = data.getStringExtra(AddTaskActivity.EXTRA_IMAGE_PATH);
            Task existing = findTaskById(taskId);
            if (existing != null) {
                String oldImagePath = existing.getImagePath();
                if (oldImagePath != null && !oldImagePath.equals(newImagePath)) {
                    imageStorage.deleteImage(oldImagePath);
                }
            }

            Task updatedTask = new Task(
                    taskId,
                    data.getStringExtra("title"),
                    data.getStringExtra("description"),
                    data.getStringExtra("category"),
                    data.getStringExtra("deadline"),
                    data.getBooleanExtra("completed", false),
                    data.getStringExtra("priority")
            );
            updatedTask.setImagePath(newImagePath);
            taskViewModel.updateTask(updatedTask);
        }
    }

    private void updateDetailOverlayVisibility() {
        boolean hasDetail = getChildFragmentManager().getBackStackEntryCount() > 0;
        if (fragmentContainer != null) fragmentContainer.setVisibility(hasDetail ? View.VISIBLE : View.GONE);
        if (detailScrim != null) {
            detailScrim.setVisibility(!isTwoPane() && hasDetail ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        getChildFragmentManager().clearFragmentResultListener(TaskDetailFragment.RESULT_KEY);
        super.onDestroyView();
        rvTasks = null;
        rvCategories = null;
        btnAddTask = null;
        btnDeleteSelected = null;
        fragmentContainer = null;
        detailScrim = null;
        taskAdapter = null;
    }
}
