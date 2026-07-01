package com.example.taskmanagerapp.ui.activity;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewParent;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.taskmanagerapp.R;
import com.example.taskmanagerapp.data.model.Category;
import com.example.taskmanagerapp.utils.ImageStorageHelper;
import com.example.taskmanagerapp.viewmodel.CategoryViewModel;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AddTaskActivity extends AppCompatActivity {
    public static final String EXTRA_IMAGE_PATH = "imagePath";
    public static final String EXTRA_FOCUS_TARGET = "focusTarget";
    public static final String EXTRA_SAVE_ON_BACK = "saveOnBack";
    public static final String FOCUS_TITLE = "title";
    public static final String FOCUS_DESCRIPTION = "description";
    public static final String FOCUS_SUBTASK = "subtask";
    public static final String FOCUS_ATTACHMENT = "attachment";
    public static final String FOCUS_CATEGORY = "category";
    public static final String FOCUS_PRIORITY = "priority";
    public static final String FOCUS_MENU = "menu";

    private static final String PRIORITY_HIGH = "High";
    private static final String PRIORITY_MEDIUM = "Medium";
    private static final String PRIORITY_LOW = "Low";
    private static final String PRIORITY_NONE = "None";

    private EditText edtTitle;
    private EditText edtDescription;
    private TextView tvEditorCategory;
    private ImageButton tvPriorityChip;
    private TextView tvDateValue;
    private TextView tvTimeValue;
    private TextView tvAttachmentValue;
    private TextView btnSave;
    private View categoryPicker;
    private View dateCard;
    private View timeCard;
    private View reminderRow;
    private View addSubtaskRow;
    private View btnPickAttachment;
    private View btnRemoveAttachment;
    private View attachmentChevron;
    private View btnCancel;
    private View btnEditorBack;
    private View btnEditorMore;
    private ScrollView editorScroll;
    private ImageView ivAttachment;

    private boolean isEdit = false;
    private int position = -1;
    private String id = null;
    private boolean isCompleted = false;
    private boolean wontDo = false;
    private String selectedCategoryId = null;
    private String selectedPriority = PRIORITY_NONE;
    private String selectedDate = null;
    private String selectedTime = null;
    private String initialFocusTarget = null;
    private boolean saveOnBack = false;

    private ImageStorageHelper storage;
    private CategoryViewModel categoryViewModel;
    private final List<CategoryOption> categoryOptions = new ArrayList<>();
    private String originalImageFile = null;
    private String currentImageFile = null;
    private final List<String> sessionFiles = new ArrayList<>();
    private ActivityResultLauncher<PickVisualMediaRequest> pickImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_task);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        storage = new ImageStorageHelper(this);
        categoryViewModel = new ViewModelProvider(this).get(CategoryViewModel.class);

        bindViews();
        observeCategories();
        restoreStateOrIntent(savedInstanceState);
        setupActions();
        updateEditorUi();
        applyInitialFocus();
    }

    private void bindViews() {
        edtTitle = findViewById(R.id.edtTitle);
        edtDescription = findViewById(R.id.edtDescription);
        tvEditorCategory = findViewById(R.id.tvEditorCategory);
        tvPriorityChip = findViewById(R.id.tvPriorityChip);
        tvDateValue = findViewById(R.id.tvDateValue);
        tvTimeValue = findViewById(R.id.tvTimeValue);
        tvAttachmentValue = findViewById(R.id.tvAttachmentValue);
        btnSave = findViewById(R.id.btnSave);
        editorScroll = findViewById(R.id.editorScroll);
        categoryPicker = findViewById(R.id.categoryPicker);
        dateCard = findViewById(R.id.dateCard);
        timeCard = findViewById(R.id.timeCard);
        reminderRow = findViewById(R.id.reminderRow);
        addSubtaskRow = findViewById(R.id.addSubtaskRow);
        btnPickAttachment = findViewById(R.id.btnPickAttachment);
        btnRemoveAttachment = findViewById(R.id.btnRemoveAttachment);
        attachmentChevron = findViewById(R.id.attachmentChevron);
        btnCancel = findViewById(R.id.btnCancel);
        btnEditorBack = findViewById(R.id.btnEditorBack);
        btnEditorMore = findViewById(R.id.btnEditorMore);
        ivAttachment = findViewById(R.id.ivAttachment);
    }

    private void setupActions() {
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), this::onPickedImage);
        btnEditorBack.setOnClickListener(v -> handleEditorBack());
        btnCancel.setOnClickListener(v -> finishWithoutSaving());
        categoryPicker.setOnClickListener(v -> showCategoryPicker());
        tvPriorityChip.setOnClickListener(v -> showPriorityMenu());
        dateCard.setOnClickListener(v -> showDatePicker());
        timeCard.setOnClickListener(v -> showTimePicker());
        reminderRow.setOnClickListener(v -> showPlaceholderToast());
        addSubtaskRow.setOnClickListener(v -> showPlaceholderToast());
        btnEditorMore.setOnClickListener(v -> showPlaceholderToast());
        btnPickAttachment.setOnClickListener(v -> pickImageLauncher.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build()));
        btnRemoveAttachment.setOnClickListener(v -> removeCurrentAttachment());
        btnSave.setOnClickListener(v -> saveTask());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishWithoutSaving();
            }
        });
    }

    private void restoreStateOrIntent(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            isEdit = savedInstanceState.getBoolean("isEdit");
            position = savedInstanceState.getInt("position");
            id = savedInstanceState.getString("id");
            isCompleted = savedInstanceState.getBoolean("isCompleted");
            wontDo = savedInstanceState.getBoolean("wontDo");
            selectedCategoryId = savedInstanceState.getString("selectedCategoryId");
            selectedPriority = normalizePriority(savedInstanceState.getString("selectedPriority"));
            selectedDate = savedInstanceState.getString("selectedDate");
            selectedTime = savedInstanceState.getString("selectedTime");
            saveOnBack = savedInstanceState.getBoolean("saveOnBack");
            originalImageFile = savedInstanceState.getString("originalImageFile");
            currentImageFile = savedInstanceState.getString("currentImageFile");
            ArrayList<String> sf = savedInstanceState.getStringArrayList("sessionFiles");
            if (sf != null) {
                sessionFiles.addAll(sf);
            }
            return;
        }

        Intent intent = getIntent();
        if (intent == null) {
            return;
        }
        isEdit = intent.getBooleanExtra("isEdit", false);
        position = intent.getIntExtra("position", -1);
        id = intent.getStringExtra("id");
        selectedCategoryId = intent.getStringExtra("categoryId");
        initialFocusTarget = intent.getStringExtra(EXTRA_FOCUS_TARGET);
        saveOnBack = intent.getBooleanExtra(EXTRA_SAVE_ON_BACK, false);
        wontDo = intent.getBooleanExtra("wontDo", false);
        parseDeadline(intent.getStringExtra("deadline"));

        if (isEdit) {
            edtTitle.setText(intent.getStringExtra("title"));
            edtDescription.setText(intent.getStringExtra("description"));
            isCompleted = intent.getBooleanExtra("completed", false);
            selectedPriority = normalizePriority(intent.getStringExtra("priority"));
            originalImageFile = intent.getStringExtra(EXTRA_IMAGE_PATH);
            currentImageFile = originalImageFile;
        }
    }

    private void observeCategories() {
        categoryViewModel.getCategories().observe(this, categories -> {
            categoryOptions.clear();
            categoryOptions.add(new CategoryOption(null, "Inbox"));
            if (categories != null) {
                for (Category category : categories) {
                    categoryOptions.add(new CategoryOption(category.getId(), category.getName()));
                }
            }
            updateCategoryTitle();
        });
    }

    private void updateEditorUi() {
        btnSave.setText(isEdit ? R.string.save_task_changes : R.string.create_task);
        updateCategoryTitle();
        updatePriorityChip();
        updateDateTimeViews();
        updateAttachmentUi();
    }

    private void applyInitialFocus() {
        if (isBlank(initialFocusTarget)) {
            return;
        }
        View target;
        boolean showKeyboard = false;
        if (FOCUS_CATEGORY.equals(initialFocusTarget)) {
            target = categoryPicker;
        } else if (FOCUS_PRIORITY.equals(initialFocusTarget)) {
            target = tvPriorityChip;
        } else if (FOCUS_MENU.equals(initialFocusTarget)) {
            target = btnEditorMore;
        } else if (FOCUS_DESCRIPTION.equals(initialFocusTarget)) {
            target = edtDescription;
            showKeyboard = true;
        } else if (FOCUS_SUBTASK.equals(initialFocusTarget)) {
            target = addSubtaskRow;
        } else if (FOCUS_ATTACHMENT.equals(initialFocusTarget)) {
            target = btnPickAttachment;
        } else {
            target = edtTitle;
            showKeyboard = true;
        }

        boolean shouldShowKeyboard = showKeyboard;
        target.post(() -> {
            scrollToEditorTarget(target);
            target.requestFocus();
            if (FOCUS_CATEGORY.equals(initialFocusTarget)) {
                showCategoryPicker();
                return;
            }
            if (FOCUS_PRIORITY.equals(initialFocusTarget)) {
                showPriorityMenu();
                return;
            }
            if (FOCUS_MENU.equals(initialFocusTarget)) {
                showPlaceholderToast();
                return;
            }
            if (shouldShowKeyboard) {
                showKeyboard(target);
                if (target instanceof EditText) {
                    EditText editText = (EditText) target;
                    editText.setSelection(editText.getText().length());
                }
            }
        });
    }

    private void scrollToEditorTarget(View target) {
        if (editorScroll == null || target == null) {
            return;
        }
        int y = target.getTop();
        ViewParent parent = target.getParent();
        while (parent instanceof View && parent != editorScroll) {
            View parentView = (View) parent;
            y += parentView.getTop();
            parent = parentView.getParent();
        }
        editorScroll.smoothScrollTo(0, Math.max(0, y - dp(16)));
    }

    private void showKeyboard(View target) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(target, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void showCategoryPicker() {
        if (categoryOptions.isEmpty()) {
            categoryOptions.add(new CategoryOption(null, "Inbox"));
        }
        String[] labels = new String[categoryOptions.size()];
        int checked = 0;
        for (int i = 0; i < categoryOptions.size(); i++) {
            CategoryOption option = categoryOptions.get(i);
            labels[i] = option.name;
            if (sameString(selectedCategoryId, option.id)) {
                checked = i;
            }
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.task_editor_category)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    selectedCategoryId = categoryOptions.get(which).id;
                    updateCategoryTitle();
                    dialog.dismiss();
                })
                .show();
    }

    private void showPriorityMenu() {
        PopupMenu menu = new PopupMenu(this, tvPriorityChip);
        menu.getMenu().add(PRIORITY_NONE);
        menu.getMenu().add(PRIORITY_HIGH);
        menu.getMenu().add(PRIORITY_MEDIUM);
        menu.getMenu().add(PRIORITY_LOW);
        menu.setOnMenuItemClickListener(item -> {
            selectedPriority = normalizePriority(item.getTitle().toString());
            updatePriorityChip();
            return true;
        });
        menu.show();
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        Calendar parsed = parseDate(selectedDate);
        if (parsed != null) {
            calendar = parsed;
        }
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar picked = Calendar.getInstance();
            picked.set(year, month, dayOfMonth);
            selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(picked.getTime());
            updateDateTimeViews();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        if (!isBlank(selectedTime)) {
            String[] parts = selectedTime.split(":");
            if (parts.length >= 2) {
                try {
                    calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(parts[0]));
                    calendar.set(Calendar.MINUTE, Integer.parseInt(parts[1]));
                } catch (NumberFormatException ignored) {
                    // Fall back to current time.
                }
            }
        }
        TimePickerDialog dialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            if (isBlank(selectedDate)) {
                selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().getTime());
            }
            selectedTime = String.format(Locale.US, "%02d:%02d", hourOfDay, minute);
            updateDateTimeViews();
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false);
        dialog.show();
    }

    private void parseDeadline(String deadline) {
        selectedDate = null;
        selectedTime = null;
        if (isBlank(deadline)) {
            return;
        }
        String trimmed = deadline.trim();
        if (trimmed.length() >= 10) {
            selectedDate = trimmed.substring(0, 10);
        }
        if (trimmed.length() >= 16 && trimmed.charAt(10) == ' ') {
            selectedTime = trimmed.substring(11, 16);
        }
    }

    private void updateCategoryTitle() {
        if (tvEditorCategory == null) {
            return;
        }
        tvEditorCategory.setText(getSelectedCategoryName());
    }

    private String getSelectedCategoryName() {
        if (isBlank(selectedCategoryId)) {
            return "Inbox";
        }
        for (CategoryOption option : categoryOptions) {
            if (sameString(selectedCategoryId, option.id)) {
                return option.name;
            }
        }
        return "Inbox";
    }

    private void updatePriorityChip() {
        selectedPriority = normalizePriority(selectedPriority);
        int color = priorityColor(selectedPriority);
        tvPriorityChip.setColorFilter(color);

        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setColor(Color.rgb(21, 26, 32));
        background.setCornerRadius(dp(18));
        background.setStroke(dp(1), color);
        tvPriorityChip.setBackground(background);
    }

    private void updateDateTimeViews() {
        tvDateValue.setText(isBlank(selectedDate) ? getString(R.string.task_editor_no_date) : displayDate(selectedDate));
        tvTimeValue.setText(isBlank(selectedTime) ? getString(R.string.task_editor_no_time) : displayTime(selectedTime));
    }

    private String displayDate(String date) {
        Calendar parsed = parseDate(date);
        if (parsed == null) {
            return date;
        }
        return new SimpleDateFormat("MMM d, yyyy", Locale.US).format(parsed.getTime());
    }

    private String displayTime(String time) {
        try {
            SimpleDateFormat input = new SimpleDateFormat("HH:mm", Locale.US);
            SimpleDateFormat output = new SimpleDateFormat("h:mm a", Locale.US);
            return output.format(input.parse(time));
        } catch (ParseException e) {
            return time;
        }
    }

    private Calendar parseDate(String date) {
        if (isBlank(date)) {
            return null;
        }
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date));
            return calendar;
        } catch (ParseException e) {
            return null;
        }
    }

    private String buildDeadline() {
        if (isBlank(selectedDate)) {
            return "";
        }
        if (isBlank(selectedTime)) {
            return selectedDate;
        }
        return selectedDate + " " + selectedTime;
    }

    private void onPickedImage(Uri uri) {
        if (uri == null) {
            return;
        }
        String name = "task_" + System.currentTimeMillis() + ".jpg";
        if (storage.saveFromUri(uri, name) == null) {
            Toast.makeText(this, R.string.task_attachment_save_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        sessionFiles.add(name);
        currentImageFile = name;
        updateAttachmentUi();
    }

    private void removeCurrentAttachment() {
        currentImageFile = null;
        updateAttachmentUi();
    }

    private void updateAttachmentUi() {
        if (currentImageFile != null) {
            Bitmap bm = storage.loadBitmapForView(currentImageFile, 800, 800);
            if (bm != null) {
                ivAttachment.setImageBitmap(bm);
                ivAttachment.setVisibility(View.VISIBLE);
            } else {
                ivAttachment.setImageDrawable(null);
                ivAttachment.setVisibility(View.GONE);
            }
            tvAttachmentValue.setText(R.string.task_attachment_label);
            btnRemoveAttachment.setVisibility(View.VISIBLE);
            attachmentChevron.setVisibility(View.GONE);
        } else {
            ivAttachment.setImageDrawable(null);
            ivAttachment.setVisibility(View.GONE);
            tvAttachmentValue.setText(R.string.task_editor_add_attachments);
            btnRemoveAttachment.setVisibility(View.GONE);
            attachmentChevron.setVisibility(View.VISIBLE);
        }
    }

    private void saveTask() {
        for (String f : sessionFiles) {
            if (!f.equals(currentImageFile)) {
                storage.deleteImage(f);
            }
        }

        Intent resultIntent = new Intent();
        resultIntent.putExtra("id", id);
        resultIntent.putExtra("title", edtTitle.getText().toString().trim());
        resultIntent.putExtra("description", edtDescription.getText().toString().trim());
        resultIntent.putExtra("categoryId", selectedCategoryId);
        resultIntent.putExtra("deadline", buildDeadline());
        resultIntent.putExtra("priority", selectedPriority);
        resultIntent.putExtra("completed", isCompleted);
        resultIntent.putExtra("wontDo", wontDo);
        resultIntent.putExtra("isEdit", isEdit);
        resultIntent.putExtra("position", position);
        resultIntent.putExtra(EXTRA_IMAGE_PATH, currentImageFile);

        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private void handleEditorBack() {
        if (saveOnBack && isEdit) {
            saveTask();
        } else {
            finishWithoutSaving();
        }
    }

    private void finishWithoutSaving() {
        cleanupOrphanFiles();
        finish();
    }

    private void cleanupOrphanFiles() {
        for (String f : sessionFiles) {
            if (!f.equals(originalImageFile)) {
                storage.deleteImage(f);
            }
        }
    }

    private void showPlaceholderToast() {
        Toast.makeText(this, R.string.task_editor_placeholder_message, Toast.LENGTH_SHORT).show();
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

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("isEdit", isEdit);
        outState.putInt("position", position);
        outState.putString("id", id);
        outState.putBoolean("isCompleted", isCompleted);
        outState.putBoolean("wontDo", wontDo);
        outState.putString("selectedCategoryId", selectedCategoryId);
        outState.putString("selectedPriority", selectedPriority);
        outState.putString("selectedDate", selectedDate);
        outState.putString("selectedTime", selectedTime);
        outState.putBoolean("saveOnBack", saveOnBack);
        outState.putString("originalImageFile", originalImageFile);
        outState.putString("currentImageFile", currentImageFile);
        outState.putStringArrayList("sessionFiles", new ArrayList<>(sessionFiles));
    }

    private static class CategoryOption {
        final String id;
        final String name;

        CategoryOption(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
