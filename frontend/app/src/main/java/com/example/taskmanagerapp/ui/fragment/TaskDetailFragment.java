package com.example.taskmanagerapp.ui.fragment;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.taskmanagerapp.R;
import com.example.taskmanagerapp.data.model.Category;
import com.example.taskmanagerapp.data.model.Task;
import com.example.taskmanagerapp.ui.activity.AddTaskActivity;
import com.example.taskmanagerapp.utils.ImageStorageHelper;
import com.example.taskmanagerapp.viewmodel.CategoryViewModel;
import com.example.taskmanagerapp.viewmodel.TaskViewModel;

import java.util.ArrayList;
import java.util.List;

public class TaskDetailFragment extends Fragment {

    private static final String ARG_TASK = "task";
    private static final String ARG_CATEGORY_NAME = "category_name";
    private static final String ARG_CATEGORY_ICON = "category_icon";
    private static final long SHEET_ANIMATION_MS = 260L;
    private static final long EDIT_TRANSITION_MS = 180L;
    private static final float TOP_BAR_TRANSITION_START = 0.82f;
    private static final float TOP_BAR_TRANSITION_END = 1f;
    private static final String PRIORITY_HIGH = "High";
    private static final String PRIORITY_MEDIUM = "Medium";
    private static final String PRIORITY_LOW = "Low";
    private static final String PRIORITY_NONE = "None";

    public static final String RESULT_KEY = "task_detail_result_key";
    public static final String RESULT_TASK_ID = "result_task_id";
    public static final String RESULT_MESSAGE = "result_message";
    public static final String RESULT_ACTION = "result_action";
    public static final String ACTION_CLOSED = "closed";
    public static final String ACTION_UPDATED = "updated";

    private Task task;
    private String categoryName;
    private String categoryIcon;
    private boolean hasSentResult = false;
    private boolean closing = false;
    private boolean editLaunching = false;
    private boolean draggingSheet = false;
    private float dragStartY = 0f;
    private int collapsedSheetHeight = 0;
    private int touchSlop = 0;
    private View scrim;
    private View sheet;
    private View content;
    private View backButton;
    private View categoryPicker;
    private View completionRow;
    private View editorContent;
    private ValueAnimator sheetHeightAnimator;
    private String pendingFocusTarget = null;
    private TaskViewModel taskViewModel;
    private CategoryViewModel categoryViewModel;
    private final List<CategoryOption> categoryOptions = new ArrayList<>();
    private ActivityResultLauncher<Intent> editTaskLauncher;

    public static TaskDetailFragment newInstance(Task task) {
        return newInstance(task, null, null);
    }

    public static TaskDetailFragment newInstance(Task task, String categoryName, String categoryIcon) {
        TaskDetailFragment fragment = new TaskDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_TASK, task);
        args.putString(ARG_CATEGORY_NAME, categoryName);
        args.putString(ARG_CATEGORY_ICON, categoryIcon);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        editTaskLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            editLaunching = false;
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                sendUpdatedResultAndClose(result.getData());
            } else {
                resetEditTransition();
            }
        });
        if (getArguments() != null) {
            task = (Task) getArguments().getSerializable(ARG_TASK);
            categoryName = getArguments().getString(ARG_CATEGORY_NAME);
            categoryIcon = getArguments().getString(ARG_CATEGORY_ICON);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task_detail, container, false);

        taskViewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);
        categoryViewModel = new ViewModelProvider(requireActivity()).get(CategoryViewModel.class);
        touchSlop = ViewConfiguration.get(requireContext()).getScaledTouchSlop();
        scrim = view.findViewById(R.id.taskDetailScrim);
        sheet = view.findViewById(R.id.detailScrollView);
        content = view.findViewById(R.id.taskDetailContent);
        backButton = view.findViewById(R.id.btnDetailBack);
        categoryPicker = view.findViewById(R.id.detailCategoryPicker);
        completionRow = view.findViewById(R.id.detailCompletionRow);
        editorContent = view.findViewById(R.id.detailEditorContent);

        TextView tvTitle = view.findViewById(R.id.tvDetailTitle);
        TextView tvDescription = view.findViewById(R.id.tvDetailDescription);
        TextView tvCategory = view.findViewById(R.id.tvDetailCategory);
        TextView tvCategoryIcon = view.findViewById(R.id.tvDetailCategoryIcon);
        TextView tvDeadline = view.findViewById(R.id.tvDetailDeadline);
        ImageView tvPriority = view.findViewById(R.id.tvDetailPriority);
        CheckBox checkComplete = view.findViewById(R.id.checkDetailComplete);
        ImageView ivAttachment = view.findViewById(R.id.ivDetailAttachment);

        bindTaskData(tvTitle, tvDescription, tvCategory, tvCategoryIcon, tvDeadline, tvPriority, checkComplete, ivAttachment);
        observeCategories(tvCategory, tvCategoryIcon);
        applyTopBarProgress(0f);
        bindInteractions(view, tvTitle, tvDescription, tvDeadline, tvPriority, checkComplete, ivAttachment);

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                sendClosedResultAndClose();
            }
        });

        view.post(this::animateSheetIn);
        return view;
    }

    private void bindTaskData(TextView tvTitle,
                              TextView tvDescription,
                              TextView tvCategory,
                              TextView tvCategoryIcon,
                              TextView tvDeadline,
                              ImageView tvPriority,
                              CheckBox checkComplete,
                              ImageView ivAttachment) {
        if (task == null) {
            return;
        }

        tvTitle.setText(isBlank(task.getTitle()) ? getString(R.string.task_editor_title_hint) : task.getTitle());
        bindDescription(tvDescription, task.getDescription());
        tvCategory.setText(isBlank(categoryName) ? "Inbox" : categoryName);
        tvCategoryIcon.setText(cleanCategoryIcon(isBlank(categoryIcon) ? "\uD83D\uDCE5" : categoryIcon));
        String deadline = task.getDeadline();
        tvDeadline.setText(isBlank(deadline) ? "Date & Reminder" : deadline);
        bindPriorityChip(tvPriority, task.getPriority());
        checkComplete.setChecked(task.isCompleted());
        checkComplete.setClickable(false);
        checkComplete.setFocusable(false);

        String img = task.getImagePath();
        if (img != null && !img.isEmpty()) {
            ImageStorageHelper storage = new ImageStorageHelper(requireContext());
            Bitmap bm = storage.loadBitmapForView(img, 1024, 1024);
            if (bm != null) {
                ivAttachment.setImageBitmap(bm);
                ivAttachment.setVisibility(View.VISIBLE);
            }
        }
    }

    private void bindDescription(TextView tvDescription, String description) {
        if (TextUtils.isEmpty(description)) {
            tvDescription.setText(R.string.task_editor_description_hint);
            tvDescription.setTextColor(Color.rgb(105, 112, 122));
        } else {
            tvDescription.setText(description);
            tvDescription.setTextColor(Color.rgb(215, 221, 229));
        }
    }

    private void bindInteractions(View root,
                                  TextView tvTitle,
                                  TextView tvDescription,
                                  TextView tvDeadline,
                                  ImageView tvPriority,
                                  CheckBox checkComplete,
                                  ImageView ivAttachment) {
        if (scrim != null) {
            scrim.setOnClickListener(v -> sendClosedResultAndClose());
        }

        bindDragTarget(sheet);
        bindDragTarget(content);
        bindDragTarget(root.findViewById(R.id.detailTopBar));
        bindCategoryTarget(categoryPicker);
        bindPriorityTarget(tvPriority);
        bindMenuTarget(root.findViewById(R.id.btnDetailMore));
        bindDragTarget(completionRow);
        bindDragTarget(tvDeadline);
        bindCompleteCheck(checkComplete);
        bindFocusedEditTarget(tvTitle, AddTaskActivity.FOCUS_TITLE);
        bindFocusedEditTarget(tvDescription, AddTaskActivity.FOCUS_DESCRIPTION);
        bindFocusedEditTarget(root.findViewById(R.id.detailSubtasksPanel), AddTaskActivity.FOCUS_SUBTASK);
        bindFocusedEditTarget(root.findViewById(R.id.detailAddSubtaskRow), AddTaskActivity.FOCUS_SUBTASK);
        bindFocusedEditTarget(ivAttachment, AddTaskActivity.FOCUS_ATTACHMENT);
    }

    private void bindDragTarget(View target) {
        if (target == null) {
            return;
        }
        target.setOnTouchListener(this::handleDetailTouch);
        target.setClickable(true);
    }

    private void bindCategoryTarget(View target) {
        if (target == null) {
            return;
        }
        target.setOnClickListener(v -> showCategoryPicker());
        target.setOnTouchListener(this::handleDetailTouch);
        target.setClickable(true);
    }

    private void bindPriorityTarget(ImageView target) {
        if (target == null) {
            return;
        }
        target.setOnClickListener(v -> showPriorityMenu(target));
        target.setOnTouchListener(this::handleDetailTouch);
        target.setClickable(true);
    }

    private void bindMenuTarget(View target) {
        if (target == null) {
            return;
        }
        target.setOnClickListener(v -> showPlaceholderToast());
        target.setOnTouchListener(this::handleDetailTouch);
        target.setClickable(true);
    }

    private void bindFocusedEditTarget(View target, String focusTarget) {
        if (target == null) {
            return;
        }
        target.setOnClickListener(v -> openEditTask(focusTarget));
        target.setOnTouchListener(this::handleDetailTouch);
        target.setClickable(true);
    }

    private void bindCompleteCheck(CheckBox checkComplete) {
        if (checkComplete == null) {
            return;
        }
        checkComplete.setClickable(true);
        checkComplete.setFocusable(true);
        checkComplete.setOnClickListener(v -> completeTaskFromDetail(checkComplete));
    }

    private void completeTaskFromDetail(CheckBox checkComplete) {
        if (task == null || task.isDeleted()) {
            return;
        }
        boolean nextCompleted = !task.isCompleted();
        task.setCompleted(nextCompleted);
        checkComplete.setChecked(nextCompleted);
        if (taskViewModel != null) {
            taskViewModel.updateTask(task);
        }
    }

    private boolean handleDetailTouch(View target, MotionEvent event) {
        if (closing || editLaunching || sheet == null) {
            return false;
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                dragStartY = event.getRawY();
                draggingSheet = false;
                cancelSheetAnimations();
                return false;
            case MotionEvent.ACTION_MOVE:
                float deltaY = event.getRawY() - dragStartY;
                if (!draggingSheet && Math.abs(deltaY) > touchSlop) {
                    draggingSheet = true;
                    target.getParent().requestDisallowInterceptTouchEvent(true);
                }
                if (draggingSheet) {
                    applyDrag(deltaY);
                    return true;
                }
                return false;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (draggingSheet) {
                    float releaseDelta = event.getRawY() - dragStartY;
                    draggingSheet = false;
                    target.getParent().requestDisallowInterceptTouchEvent(false);
                    finishDrag(releaseDelta);
                    return true;
                }
                return false;
            default:
                return false;
        }
    }

    private void applyDrag(float deltaY) {
        if (deltaY > 0f) {
            setSheetExpansion(0f);
            float clamped = Math.min(deltaY, sheet.getHeight());
            sheet.setTranslationY(clamped);
            if (scrim != null) {
                scrim.setAlpha(Math.max(0f, 1f - clamped / Math.max(1f, sheet.getHeight() * 0.65f)));
            }
            applyEditProgress(0f);
            applyTopBarProgress(0f);
            return;
        }

        float expansionProgress = upwardExpansionProgress(deltaY);
        setSheetExpansion(expansionProgress);
        sheet.setTranslationY(0f);
        if (scrim != null) {
            scrim.setAlpha(1f);
        }

        // Áp dụng hoạt ảnh khi đạt đủ tiến trình đặt ra
        float editorProgress = topBarProgressForExpansion(expansionProgress);
        applyEditProgress(editorProgress);
        applyTopBarProgress(editorProgress);
    }

    private void finishDrag(float deltaY) {
        if (deltaY >= dp(96)) {
            sendClosedResultAndClose();
        } else if (upwardExpansionProgress(deltaY) >= 0.3f) {
            openEditTask();
        } else {
            resetEditTransition();
        }
    }

    private void applyEditProgress(float progress) {
        float clamped = Math.max(0f, Math.min(1f, progress));
        if (completionRow != null) {
            completionRow.setAlpha(1f - clamped);
            completionRow.setTranslationY(-dp(8) * clamped);
        }
        if (editorContent != null) {
            editorContent.setZ(4f);
            editorContent.setTranslationY(-editCoverDistance() * clamped);
        }
    }

    private void resetEditTransition() {
        cancelSheetAnimations();
        animateSheetExpansion(0f, 160L);
        if (sheet != null) {
            sheet.animate()
                    .translationY(0f)
                    .setDuration(160L)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }
        if (scrim != null) {
            scrim.animate()
                    .alpha(1f)
                    .setDuration(160L)
                    .start();
        }
        if (completionRow != null) {
            completionRow.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(160L)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }
        if (editorContent != null) {
            editorContent.animate()
                    .translationY(0f)
                    .setDuration(160L)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }
        animateTopBarProgress(0f, 160L);
    }

    private void cancelSheetAnimations() {
        if (sheet != null) {
            sheet.animate().cancel();
        }
        if (scrim != null) {
            scrim.animate().cancel();
        }
        if (completionRow != null) {
            completionRow.animate().cancel();
        }
        if (editorContent != null) {
            editorContent.animate().cancel();
        }
        if (backButton != null) {
            backButton.animate().cancel();
        }
        if (categoryPicker != null) {
            categoryPicker.animate().cancel();
        }
        if (sheetHeightAnimator != null) {
            sheetHeightAnimator.cancel();
            sheetHeightAnimator = null;
        }
    }

    private int editCoverDistance() {
        if (editorContent == null || editorContent.getTop() <= 0) {
            return dp(68);
        }
        return editorContent.getTop();
    }

    private float upwardExpansionProgress(float deltaY) {
        if (deltaY >= 0f) {
            return 0f;
        }
        int travel = Math.max(dp(180), expandedSheetHeight() - collapsedSheetHeight());
        return Math.max(0f, Math.min(1f, -deltaY / Math.max(1f, travel)));
    }

    /**
     * Tính mức độ tiến triển của top bar
     * @param expansionProgress
     * @return
     */
    private float topBarProgressForExpansion(float expansionProgress) {
        float start = TOP_BAR_TRANSITION_START;
        float end = TOP_BAR_TRANSITION_END;
        return Math.max(0f, Math.min(1f, (expansionProgress - start) / (end - start)));
    }

    private void setSheetExpansion(float progress) {
        if (sheet == null) {
            return;
        }
        ensureCollapsedSheetHeight();
        int collapsedHeight = collapsedSheetHeight();
        int expandedHeight = expandedSheetHeight();
        int height = Math.round(collapsedHeight + ((expandedHeight - collapsedHeight) * Math.max(0f, Math.min(1f, progress))));
        setSheetHeight(height);
    }

    private void animateSheetExpansion(float progress, long duration) {
        if (sheet == null) {
            return;
        }
        ensureCollapsedSheetHeight();
        int collapsedHeight = collapsedSheetHeight();
        int expandedHeight = expandedSheetHeight();
        int targetHeight = Math.round(collapsedHeight + ((expandedHeight - collapsedHeight) * Math.max(0f, Math.min(1f, progress))));
        if (sheetHeightAnimator != null) {
            sheetHeightAnimator.cancel();
        }
        sheetHeightAnimator = ValueAnimator.ofInt(sheet.getHeight(), targetHeight);
        sheetHeightAnimator.setDuration(duration);
        sheetHeightAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        sheetHeightAnimator.addUpdateListener(animation -> setSheetHeight((int) animation.getAnimatedValue()));
        sheetHeightAnimator.start();
    }

    private void setSheetHeight(int height) {
        ViewGroup.LayoutParams params = sheet.getLayoutParams();
        if (params == null || params.height == height) {
            return;
        }
        params.height = height;
        sheet.setLayoutParams(params);
    }

    private void ensureCollapsedSheetHeight() {
        if (collapsedSheetHeight <= 0 && sheet != null) {
            collapsedSheetHeight = sheet.getHeight();
        }
    }

    private int collapsedSheetHeight() {
        ensureCollapsedSheetHeight();
        return Math.max(dp(320), collapsedSheetHeight);
    }

    private int expandedSheetHeight() {
        if (sheet == null || sheet.getParent() == null) {
            return collapsedSheetHeight();
        }
        View parent = (View) sheet.getParent();
        return Math.max(collapsedSheetHeight(), parent.getHeight());
    }

    private void applyTopBarProgress(float progress) {
        float clamped = Math.max(0f, Math.min(1f, progress));
        if (backButton != null) {
            backButton.setAlpha(clamped);
            backButton.setScaleX(0.84f + (0.16f * clamped));
            backButton.setScaleY(0.84f + (0.16f * clamped));
            backButton.setTranslationX(-dp(10) * (1f - clamped));
        }
        if (categoryPicker != null) {
            categoryPicker.setTranslationX(categoryEditShift() * clamped);
        }
    }

    private void animateTopBarProgress(float progress, long duration) {
        float clamped = Math.max(0f, Math.min(1f, progress));
        AccelerateDecelerateInterpolator interpolator = new AccelerateDecelerateInterpolator();
        if (backButton != null) {
            backButton.animate()
                    .alpha(clamped)
                    .scaleX(0.84f + (0.16f * clamped))
                    .scaleY(0.84f + (0.16f * clamped))
                    .translationX(-dp(10) * (1f - clamped))
                    .setDuration(duration)
                    .setInterpolator(interpolator)
                    .start();
        }
        if (categoryPicker != null) {
            categoryPicker.animate()
                    .translationX(categoryEditShift() * clamped)
                    .setDuration(duration)
                    .setInterpolator(interpolator)
                    .start();
        }
    }

    private int categoryEditShift() {
        return dp(34);
    }

    private void bindPriorityChip(ImageView chip, String priority) {
        String normalized = normalizePriority(priority);
        int color = priorityColor(normalized);
        chip.setColorFilter(color);
        chip.setBackgroundResource(R.drawable.bg_task_editor_icon_button);
    }

    private void observeCategories(TextView tvCategory, TextView tvCategoryIcon) {
        if (categoryViewModel == null) {
            return;
        }
        categoryViewModel.getCategories().observe(getViewLifecycleOwner(), categories -> {
            categoryOptions.clear();
            categoryOptions.add(new CategoryOption(null, "Inbox", "\uD83D\uDCE5"));
            if (categories != null) {
                for (Category category : categories) {
                    categoryOptions.add(new CategoryOption(category.getId(), category.getName(), cleanCategoryIcon(category.getIcon())));
                }
            }
            updateDetailCategoryTitle(tvCategory, tvCategoryIcon);
        });
    }

    private void showCategoryPicker() {
        if (task == null || task.isDeleted()) {
            return;
        }
        if (categoryOptions.isEmpty()) {
            categoryOptions.add(new CategoryOption(null, "Inbox", "\uD83D\uDCE5"));
        }

        String[] labels = new String[categoryOptions.size()];
        int checked = 0;
        for (int i = 0; i < categoryOptions.size(); i++) {
            CategoryOption option = categoryOptions.get(i);
            labels[i] = option.icon + "  " + option.name;
            if (sameString(task.getCategoryId(), option.id)) {
                checked = i;
            }
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.task_editor_category)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    CategoryOption option = categoryOptions.get(which);
                    task.setCategoryId(option.id);
                    categoryName = option.name;
                    categoryIcon = option.icon;
                    updateDetailCategoryTitle();
                    if (taskViewModel != null) {
                        taskViewModel.updateTask(task);
                    }
                    dialog.dismiss();
                })
                .show();
    }

    private void showPriorityMenu(ImageView anchor) {
        if (task == null || task.isDeleted() || anchor == null) {
            return;
        }
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        menu.getMenu().add(PRIORITY_NONE);
        menu.getMenu().add(PRIORITY_HIGH);
        menu.getMenu().add(PRIORITY_MEDIUM);
        menu.getMenu().add(PRIORITY_LOW);
        menu.setOnMenuItemClickListener(item -> {
            String selectedPriority = normalizePriority(item.getTitle().toString());
            task.setPriority(selectedPriority);
            bindPriorityChip(anchor, selectedPriority);
            if (taskViewModel != null) {
                taskViewModel.updateTask(task);
            }
            return true;
        });
        menu.show();
    }

    private void showPlaceholderToast() {
        Toast.makeText(requireContext(), R.string.task_editor_placeholder_message, Toast.LENGTH_SHORT).show();
    }

    private void updateDetailCategoryTitle() {
        View view = getView();
        if (view == null) {
            return;
        }
        updateDetailCategoryTitle(view.findViewById(R.id.tvDetailCategory), view.findViewById(R.id.tvDetailCategoryIcon));
    }

    private void updateDetailCategoryTitle(TextView tvCategory, TextView tvCategoryIcon) {
        if (tvCategory == null) {
            return;
        }
        categoryName = getSelectedCategoryName();
        tvCategory.setText(categoryName);
        categoryIcon = getSelectedCategoryIcon();
        if (tvCategoryIcon != null) {
            tvCategoryIcon.setText(categoryIcon);
        }
    }

    private String getSelectedCategoryName() {
        if (task == null || isBlank(task.getCategoryId())) {
            return "Inbox";
        }
        for (CategoryOption option : categoryOptions) {
            if (sameString(task.getCategoryId(), option.id)) {
                return option.name;
            }
        }
        return isBlank(categoryName) ? "Inbox" : categoryName;
    }

    private String getSelectedCategoryIcon() {
        if (task == null || isBlank(task.getCategoryId())) {
            return "\uD83D\uDCE5";
        }
        for (CategoryOption option : categoryOptions) {
            if (sameString(task.getCategoryId(), option.id)) {
                return cleanCategoryIcon(option.icon);
            }
        }
        return cleanCategoryIcon(categoryIcon);
    }

    private String cleanCategoryIcon(String icon) {
        String cleanIcon = icon == null ? "" : icon.trim();
        if (cleanIcon.isEmpty() || "#".equals(cleanIcon)) {
            return "\uD83D\uDCCB";
        }
        return cleanIcon;
    }

    private void openEditTask() {
        openEditTask(null);
    }

    private void openEditTask(@Nullable String focusTarget) {
        if (task == null || task.isDeleted() || editLaunching || closing) {
            return;
        }
        editLaunching = true;
        pendingFocusTarget = focusTarget;
        draggingSheet = false;
        cancelSheetAnimations();
        if (sheet != null) {
            sheet.setTranslationY(0f);
        }
        if (scrim != null) {
            scrim.setAlpha(1f);
        }
        animateSheetExpansion(1f, EDIT_TRANSITION_MS);
        if (completionRow != null) {
            completionRow.animate()
                    .alpha(0f)
                    .translationY(-dp(8))
                    .setDuration(EDIT_TRANSITION_MS)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        }
        if (editorContent != null) {
            editorContent.setZ(4f);
            editorContent.animate()
                    .translationY(-editCoverDistance())
                    .setDuration(EDIT_TRANSITION_MS)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        }
        animateTopBarProgress(1f, EDIT_TRANSITION_MS);

        View launchAnchor = sheet == null ? content : sheet;
        if (launchAnchor == null) {
            launchEditTask();
        } else {
            launchAnchor.postDelayed(this::launchEditTask, EDIT_TRANSITION_MS);
        }
    }

    private void launchEditTask() {
        if (!isAdded() || task == null || closing) {
            editLaunching = false;
            return;
        }
        Intent editIntent = new Intent(requireContext(), AddTaskActivity.class);
        editIntent.putExtra("id", task.getId());
        editIntent.putExtra("title", task.getTitle());
        editIntent.putExtra("description", task.getDescription());
        editIntent.putExtra("categoryId", task.getCategoryId());
        editIntent.putExtra("priority", task.getPriority());
        editIntent.putExtra("deadline", task.getDeadline());
        editIntent.putExtra("completed", task.isCompleted());
        editIntent.putExtra("wontDo", task.isWontDo());
        editIntent.putExtra("isEdit", true);
        editIntent.putExtra(AddTaskActivity.EXTRA_IMAGE_PATH, task.getImagePath());
        editIntent.putExtra(AddTaskActivity.EXTRA_SAVE_ON_BACK, true);
        if (!isBlank(pendingFocusTarget)) {
            editIntent.putExtra(AddTaskActivity.EXTRA_FOCUS_TARGET, pendingFocusTarget);
        }
        pendingFocusTarget = null;
        ActivityOptionsCompat options = ActivityOptionsCompat.makeCustomAnimation(requireContext(), 0, 0);
        editTaskLauncher.launch(editIntent, options);
    }

    private void sendClosedResultAndClose() {
        Bundle result = new Bundle();
        result.putString(RESULT_ACTION, ACTION_CLOSED);
        if (!hasSentResult) {
            if (task != null) {
                result.putString(RESULT_TASK_ID, task.getId());
                result.putString(RESULT_MESSAGE, "Viewed: " + task.getTitle());
            } else {
                result.putString(RESULT_MESSAGE, "Closed task detail");
            }
        }
        closeWithResult(result);
    }

    private void sendUpdatedResultAndClose(Intent data) {
        Bundle result = new Bundle();
        result.putString(RESULT_ACTION, ACTION_UPDATED);
        result.putString(RESULT_TASK_ID, valueOrFallback(data.getStringExtra("id"), task == null ? null : task.getId()));
        result.putString("title", data.getStringExtra("title"));
        result.putString("description", data.getStringExtra("description"));
        result.putString("categoryId", data.getStringExtra("categoryId"));
        result.putString("deadline", data.getStringExtra("deadline"));
        result.putString("priority", data.getStringExtra("priority"));
        result.putBoolean("completed", data.getBooleanExtra("completed", task != null && task.isCompleted()));
        result.putBoolean("wontDo", data.getBooleanExtra("wontDo", task != null && task.isWontDo()));
        result.putString(AddTaskActivity.EXTRA_IMAGE_PATH, data.getStringExtra(AddTaskActivity.EXTRA_IMAGE_PATH));
        closeWithResult(result);
    }

    private String valueOrFallback(String value, String fallback) {
        return value == null ? fallback : value;
    }

    private void closeWithResult(Bundle result) {
        if (closing) {
            return;
        }
        closing = true;
        if (!hasSentResult) {
            getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
            hasSentResult = true;
        }
        animateSheetOut();
    }

    private void animateSheetIn() {
        if (sheet == null) {
            return;
        }
        if (scrim != null) {
            scrim.setAlpha(0f);
            scrim.animate()
                    .alpha(1f)
                    .setDuration(SHEET_ANIMATION_MS)
                    .start();
        }
        sheet.setTranslationY(sheet.getHeight());
        sheet.animate()
                .translationY(0f)
                .setDuration(SHEET_ANIMATION_MS)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void animateSheetOut() {
        Runnable popBackStack = () -> getParentFragmentManager().popBackStack();
        if (sheet == null) {
            popBackStack.run();
            return;
        }
        if (scrim != null) {
            scrim.animate()
                    .alpha(0f)
                    .setDuration(SHEET_ANIMATION_MS)
                    .start();
        }
        sheet.animate()
                .translationY(sheet.getHeight())
                .setDuration(SHEET_ANIMATION_MS)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(popBackStack)
                .start();
    }

    private String normalizePriority(String priority) {
        if (TextUtils.isEmpty(priority)) {
            return PRIORITY_NONE;
        }
        if (PRIORITY_HIGH.equalsIgnoreCase(priority)) {
            return PRIORITY_HIGH;
        }
        if (PRIORITY_MEDIUM.equalsIgnoreCase(priority)) {
            return PRIORITY_MEDIUM;
        }
        if (PRIORITY_LOW.equalsIgnoreCase(priority)) {
            return PRIORITY_LOW;
        }
        return PRIORITY_NONE;
    }

    private int priorityColor(String priority) {
        String normalized = normalizePriority(priority);
        if (PRIORITY_HIGH.equals(normalized)) {
            return Color.rgb(255, 92, 92);
        }
        if (PRIORITY_MEDIUM.equals(normalized)) {
            return Color.rgb(255, 184, 77);
        }
        if (PRIORITY_LOW.equals(normalized)) {
            return Color.rgb(39, 189, 183);
        }
        return Color.rgb(143, 150, 158);
    }

    private boolean sameString(String first, String second) {
        return first == null ? second == null : first.equals(second);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static class CategoryOption {
        final String id;
        final String name;
        final String icon;

        CategoryOption(String id, String name, String icon) {
            this.id = id;
            this.name = name;
            this.icon = icon;
        }
    }
}
