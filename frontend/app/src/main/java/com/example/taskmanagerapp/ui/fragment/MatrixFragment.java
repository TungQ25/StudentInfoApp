package com.example.taskmanagerapp.ui.fragment;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.taskmanagerapp.R;
import com.example.taskmanagerapp.data.model.Category;
import com.example.taskmanagerapp.data.model.Task;
import com.example.taskmanagerapp.ui.activity.AddTaskActivity;
import com.example.taskmanagerapp.ui.activity.MainActivity;
import com.example.taskmanagerapp.utils.ImageStorageHelper;
import com.example.taskmanagerapp.viewmodel.CategoryViewModel;
import com.example.taskmanagerapp.viewmodel.TaskViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MatrixFragment extends Fragment implements MainActivity.TaskToolbarController {
    private static final String PRIORITY_HIGH = "High";
    private static final String PRIORITY_MEDIUM = "Medium";
    private static final String PRIORITY_LOW = "Low";
    private static final String PRIORITY_NONE = "None";

    private LinearLayout listHigh;
    private LinearLayout listMedium;
    private LinearLayout listLow;
    private LinearLayout listNone;
    private FloatingActionButton btnAddTask;
    private TaskViewModel taskViewModel;
    private CategoryViewModel categoryViewModel;
    private ImageStorageHelper imageStorage;
    private ActivityResultLauncher<Intent> addTaskLauncher;
    private final List<Task> allTasks = new ArrayList<>();
    private final List<Category> categories = new ArrayList<>();
    private boolean showCompleted = true;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addTaskLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Intent data = result.getData();
                Task task = new Task(
                        data.getStringExtra("title"),
                        data.getStringExtra("description"),
                        data.getStringExtra("categoryId"),
                        data.getStringExtra("deadline"),
                        data.getBooleanExtra("completed", false),
                        normalizePriority(data.getStringExtra("priority"))
                );
                task.setWontDo(data.getBooleanExtra("wontDo", false));
                task.setImagePath(data.getStringExtra(AddTaskActivity.EXTRA_IMAGE_PATH));
                taskViewModel.addTask(task);
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_matrix, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        listHigh = view.findViewById(R.id.listHigh);
        listMedium = view.findViewById(R.id.listMedium);
        listLow = view.findViewById(R.id.listLow);
        listNone = view.findViewById(R.id.listNone);
        btnAddTask = view.findViewById(R.id.btnMatrixAddTask);
        taskViewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);
        categoryViewModel = new ViewModelProvider(requireActivity()).get(CategoryViewModel.class);
        imageStorage = new ImageStorageHelper(requireContext());

        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).showMatrixToolbar(getString(R.string.title_matrix), this);
        }

        btnAddTask.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AddTaskActivity.class);
            intent.putExtra(AddTaskActivity.EXTRA_FOCUS_TARGET, AddTaskActivity.FOCUS_TITLE);
            addTaskLauncher.launch(intent);
        });
        setupTaskDetailResultListener();
        observeData();
        taskViewModel.syncTasks();
    }

    private void observeData() {
        taskViewModel.getTasks().observe(getViewLifecycleOwner(), tasks -> {
            allTasks.clear();
            if (tasks != null) allTasks.addAll(tasks);
            renderMatrix();
        });
        categoryViewModel.getCategories().observe(getViewLifecycleOwner(), result -> {
            categories.clear();
            if (result != null) categories.addAll(result);
        });
    }

    /**
     * Chia task vào 4 nhóm
     */
    private void renderMatrix() {
        if (listHigh == null) return;

        List<Task> high = new ArrayList<>();
        List<Task> medium = new ArrayList<>();
        List<Task> low = new ArrayList<>();
        List<Task> none = new ArrayList<>();
        for (Task task : allTasks) {
            if (!shouldShowInMatrix(task)) continue; // task null, trash/deleted hoặc wontDo thì bỏ qua
            String priority = normalizePriority(task.getPriority());
            if (PRIORITY_HIGH.equals(priority)) high.add(task);
            else if (PRIORITY_MEDIUM.equals(priority)) medium.add(task);
            else if (PRIORITY_LOW.equals(priority)) low.add(task);
            else none.add(task);
        }

        // sort riêng theo từng nhóm
        sortMatrixTasks(high);
        sortMatrixTasks(medium);
        sortMatrixTasks(low);
        sortMatrixTasks(none);

        // hiển thị từng nhóm lên giao diện
        renderQuadrant(listHigh, high);
        renderQuadrant(listMedium, medium);
        renderQuadrant(listLow, low);
        renderQuadrant(listNone, none);
    }

    private boolean shouldShowInMatrix(Task task) {
        if (task == null || task.isDeleted() || task.isWontDo()) {
            return false;
        }
        return showCompleted || !task.isCompleted();
    }

    /**
     * Sắp xếp thứ tự task trong từng matrix, task chưa completed lên trc, deadline, title
     * @param tasks
     */
    private void sortMatrixTasks(List<Task> tasks) {
        Collections.sort(tasks, Comparator
                .comparing(Task::isCompleted)
                .thenComparing(task -> task.getDeadline() == null ? "" : task.getDeadline())
                .thenComparing(task -> task.getTitle() == null ? "" : task.getTitle()));
    }

    /**
     * Render task vào một khu vực cụ thể
     * @param container
     * @param tasks
     */
    private void renderQuadrant(LinearLayout container, List<Task> tasks) {
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (Task task : tasks) {
            View row = inflater.inflate(R.layout.list_item_matrix_task, container, false);
            bindTaskRow(row, task);
            container.addView(row);
        }
    }

    /**
     * Gắn dữ liệu của task vào từng dòng UI
     * @param row
     * @param task
     */
    private void bindTaskRow(View row, Task task) {
        CheckBox checkBox = row.findViewById(R.id.checkMatrixComplete);
        TextView tvTitle = row.findViewById(R.id.tvMatrixTaskTitle);
        TextView tvDeadline = row.findViewById(R.id.tvMatrixTaskDeadline);

        tvTitle.setText(task.getTitle());

        // Không có deadline thì ẩn đi
        String deadline = task.getDeadline();
        if (deadline == null || deadline.trim().isEmpty()) {
            tvDeadline.setVisibility(View.GONE);
        } else {
            tvDeadline.setVisibility(View.VISIBLE);
            tvDeadline.setText(deadline);
        }

        checkBox.setOnCheckedChangeListener(null);
        checkBox.setChecked(task.isCompleted());
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            task.setCompleted(isChecked);
            taskViewModel.updateTask(task);
        });

        row.setAlpha(task.isCompleted() ? 0.42f : 1f); // làm mờ task completed
        row.setOnClickListener(v -> openTaskDetail(task));
    }

    private void openTaskDetail(Task task) {
        if (task == null || !(requireActivity() instanceof MainActivity)) {
            return;
        }
        TaskDetailFragment fragment = TaskDetailFragment.newInstance(
                task,
                getCategoryName(task.getCategoryId()),
                getCategoryIcon(task.getCategoryId())
        );
        ((MainActivity) requireActivity()).showFullScreenFragment(fragment);
    }

    private void setupTaskDetailResultListener() {
        getParentFragmentManager().setFragmentResultListener(TaskDetailFragment.RESULT_KEY, getViewLifecycleOwner(), (requestKey, result) -> {
            String action = result.getString(TaskDetailFragment.RESULT_ACTION);
            if (TaskDetailFragment.ACTION_UPDATED.equals(action)) {
                updateTaskFromDetailResult(result);
            }
        });
    }

    private void updateTaskFromDetailResult(Bundle result) {
        String taskId = result.getString(TaskDetailFragment.RESULT_TASK_ID);
        Task existing = findTaskById(taskId);
        if (existing == null) {
            return;
        }

        String newImagePath = result.containsKey(AddTaskActivity.EXTRA_IMAGE_PATH)
                ? result.getString(AddTaskActivity.EXTRA_IMAGE_PATH)
                : existing.getImagePath();
        String oldImagePath = existing.getImagePath();
        if (!sameString(oldImagePath, newImagePath) && oldImagePath != null && !oldImagePath.isEmpty()) {
            imageStorage.deleteImage(oldImagePath);
        }

        Task updatedTask = new Task(
                existing.getId(),
                resultString(result, "title", existing.getTitle()),
                resultString(result, "description", existing.getDescription()),
                resultString(result, "categoryId", existing.getCategoryId()),
                resultString(result, "deadline", existing.getDeadline()),
                result.containsKey("completed") ? result.getBoolean("completed") : existing.isCompleted(),
                result.containsKey("wontDo") ? result.getBoolean("wontDo") : existing.isWontDo(),
                normalizePriority(resultString(result, "priority", existing.getPriority())),
                newImagePath,
                existing.getUpdatedAt(),
                existing.isSynced(),
                existing.isDeleted(),
                existing.isPermanentDeletePending(),
                existing.getUserId());
        taskViewModel.updateTask(updatedTask);
    }

    private String resultString(Bundle result, String key, String fallback) {
        return result.containsKey(key) ? result.getString(key) : fallback;
    }

    private boolean sameString(String first, String second) {
        return first == null ? second == null : first.equals(second);
    }

    private Task findTaskById(String taskId) {
        for (Task task : allTasks) {
            if (taskId.equals(task.getId())) return task;
        }
        return null;
    }

    private String getCategoryName(String categoryId) {
        if (categoryId == null || categoryId.trim().isEmpty()) return "Inbox";
        for (Category category : categories) {
            if (categoryId.equals(category.getId())) return category.getName();
        }
        return "Inbox";
    }

    private String getCategoryIcon(String categoryId) {
        if (categoryId == null || categoryId.trim().isEmpty()) return "#";
        for (Category category : categories) {
            if (categoryId.equals(category.getId())) {
                String icon = category.getIcon();
                return icon == null || icon.trim().isEmpty() ? "#" : icon;
            }
        }
        return "#";
    }

    /**
     * Chuẩn hoá priority tránh lỗi do bị sai chữ hoa/thường hoặc bị null
     * @param priority
     * @return
     */
    private String normalizePriority(String priority) {
        if (priority == null || priority.trim().isEmpty()) return PRIORITY_NONE;
        if (PRIORITY_HIGH.equalsIgnoreCase(priority)) return PRIORITY_HIGH;
        if (PRIORITY_MEDIUM.equalsIgnoreCase(priority)) return PRIORITY_MEDIUM;
        if (PRIORITY_LOW.equalsIgnoreCase(priority)) return PRIORITY_LOW;
        return PRIORITY_NONE;
    }

    @Override
    public void onToggleSidebarRequested() {
        // Matrix không dùng task sidebar.
    }

    @Override
    public void onToolbarMoreRequested(View anchor) {
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        menu.getMenu().add(showCompleted ? "Hide completed" : "Show completed");
        menu.setOnMenuItemClickListener(item -> {
            showCompleted = !showCompleted;
            renderMatrix();
            Toast.makeText(requireContext(), showCompleted ? "Showing completed" : "Completed hidden", Toast.LENGTH_SHORT).show();
            return true;
        });
        menu.show();
    }

    @Override
    public void onDestroyView() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).clearTaskToolbarController(this);
        }
        super.onDestroyView();
        listHigh = null;
        listMedium = null;
        listLow = null;
        listNone = null;
        btnAddTask = null;
    }
}
