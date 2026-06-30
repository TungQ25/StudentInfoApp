package com.example.taskmanagerapp.ui.activity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.taskmanagerapp.R;
import com.example.taskmanagerapp.data.model.Category;
import com.example.taskmanagerapp.utils.ImageStorageHelper;
import com.example.taskmanagerapp.viewmodel.CategoryViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AddTaskActivity extends AppCompatActivity {
    public static final String EXTRA_IMAGE_PATH = "imagePath";

    EditText edtTitle, edtDescription, edtDeadline;
    Spinner spinnerCategory;
    RadioGroup rgPriority;
    Button btnSave, btnPickAttachment, btnRemoveAttachment;
    ImageView ivAttachment;

    boolean isEdit = false;
    int position = -1;
    String id = null;
    boolean isCompleted = false;
    boolean wontDo = false;
    String selectedCategoryId = null;

    ImageStorageHelper storage;
    CategoryViewModel categoryViewModel;
    ArrayAdapter<CategoryOption> categoryAdapter;
    final List<CategoryOption> categoryOptions = new ArrayList<>();
    String originalImageFile = null;
    String currentImageFile = null;
    final List<String> sessionFiles = new ArrayList<>();
    ActivityResultLauncher<PickVisualMediaRequest> pickImageLauncher;

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

        edtTitle = findViewById(R.id.edtTitle);
        edtDescription = findViewById(R.id.edtDescription);
        edtDeadline = findViewById(R.id.edtDeadline);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        rgPriority = findViewById(R.id.rgPriority);
        btnSave = findViewById(R.id.btnSave);
        btnPickAttachment = findViewById(R.id.btnPickAttachment);
        btnRemoveAttachment = findViewById(R.id.btnRemoveAttachment);
        ivAttachment = findViewById(R.id.ivAttachment);

        categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoryOptions);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);
        observeCategories();

        restoreStateOrIntent(savedInstanceState);

        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), this::onPickedImage);
        btnPickAttachment.setOnClickListener(v -> pickImageLauncher.launch(new PickVisualMediaRequest.Builder().setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build()));
        btnRemoveAttachment.setOnClickListener(v -> removeCurrentAttachment());
        edtDeadline.setFocusable(false);
        edtDeadline.setOnClickListener(v -> showDatePicker());
        btnSave.setOnClickListener(v -> saveTask());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                cleanupOrphanFiles();
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
            }
        });
    }

    /**
     * khôi phục dữ liệu cho màn hình Add/Edit Task
     * @param savedInstanceState
     */
    private void restoreStateOrIntent(Bundle savedInstanceState) {
        // Load lại dữ liệu khi hệ thống bị tạo lại
        if (savedInstanceState != null) {
            isEdit = savedInstanceState.getBoolean("isEdit");
            position = savedInstanceState.getInt("position");
            id = savedInstanceState.getString("id");
            isCompleted = savedInstanceState.getBoolean("isCompleted");
            wontDo = savedInstanceState.getBoolean("wontDo");
            selectedCategoryId = savedInstanceState.getString("selectedCategoryId");
            originalImageFile = savedInstanceState.getString("originalImageFile");
            currentImageFile = savedInstanceState.getString("currentImageFile");
            ArrayList<String> sf = savedInstanceState.getStringArrayList("sessionFiles");
            if (sf != null) sessionFiles.addAll(sf);
            updateAttachmentUi();
            return;
        }

        // Load dữ liệu từ Intent khi mở màn hình lần đầu
        Intent intent = getIntent();
        if (intent == null) return;
        isEdit = intent.getBooleanExtra("isEdit", false);
        position = intent.getIntExtra("position", -1);
        id = intent.getStringExtra("id");
        selectedCategoryId = intent.getStringExtra("categoryId");
        wontDo = intent.getBooleanExtra("wontDo", false);
        String deadline = intent.getStringExtra("deadline");

        // Sửa task thì load dữ liệu cũ lên form
        if (isEdit) {
            edtTitle.setText(intent.getStringExtra("title"));
            edtDescription.setText(intent.getStringExtra("description"));
            edtDeadline.setText(deadline);
            isCompleted = intent.getBooleanExtra("completed", false);
            String priority = intent.getStringExtra("priority");
            if ("High".equals(priority)) rgPriority.check(R.id.rbHigh);
            else if ("Medium".equals(priority)) rgPriority.check(R.id.rbMedium);
            else if ("Low".equals(priority)) rgPriority.check(R.id.rbLow);
            else rgPriority.check(R.id.rbNone);
            originalImageFile = intent.getStringExtra(EXTRA_IMAGE_PATH);
            currentImageFile = originalImageFile;
            updateAttachmentUi();
        } else if (deadline != null) {
            edtDeadline.setText(deadline);
        }
    }

    /**
     * Tự động cập nhật khi category thay đổi
     */
    private void observeCategories() {
        categoryViewModel.getCategories().observe(this, categories -> {
            categoryOptions.clear();
            categoryOptions.add(new CategoryOption(null, "Inbox"));
            if (categories != null) {
                for (Category category : categories) {
                    categoryOptions.add(new CategoryOption(category.getId(), category.getName()));
                }
            }
            categoryAdapter.notifyDataSetChanged();
            selectCategoryId(selectedCategoryId);
        });
    }

    private void selectCategoryId(String categoryId) {
        for (int i = 0; i < categoryOptions.size(); i++) {
            CategoryOption option = categoryOptions.get(i);
            if ((categoryId == null && option.id == null) || (categoryId != null && categoryId.equals(option.id))) {
                spinnerCategory.setSelection(i);
                return;
            }
        }
        spinnerCategory.setSelection(0);
    }

    /**
     * Mở hộp thoại chọn ngày
     */
    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(year, month, dayOfMonth);
            edtDeadline.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.getTime()));
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void onPickedImage(Uri uri) {
        if (uri == null) return;
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
                ivAttachment.setVisibility(View.GONE);
            }
            btnRemoveAttachment.setEnabled(true);
        } else {
            ivAttachment.setImageDrawable(null);
            ivAttachment.setVisibility(View.GONE);
            btnRemoveAttachment.setEnabled(false);
        }
    }

    private void saveTask() {
        String title = edtTitle.getText().toString().trim();
        String description = edtDescription.getText().toString().trim();
        String deadline = edtDeadline.getText().toString().trim();
        CategoryOption option = (CategoryOption) spinnerCategory.getSelectedItem();
        String categoryId = option == null ? null : option.id;

        int selectedPriorityId = rgPriority.getCheckedRadioButtonId();
        RadioButton rbSelected = findViewById(selectedPriorityId);
        String priority = rbSelected == null ? "None" : rbSelected.getText().toString();

        // Files được chọn trong session nhưng không được chọn (ví dụ người dùng chọn rồi chọn
        // ảnh khác, hoặc nhấn Xóa) -> xóa để tránh orphan trong storage.
        for (String f : sessionFiles) {
            if (!f.equals(currentImageFile)) {
                storage.deleteImage(f);
            }
        }

        Intent resultIntent = new Intent();
        resultIntent.putExtra("id", id);
        resultIntent.putExtra("title", title);
        resultIntent.putExtra("description", description);
        resultIntent.putExtra("categoryId", categoryId);
        resultIntent.putExtra("deadline", deadline);
        resultIntent.putExtra("priority", priority);
        resultIntent.putExtra("completed", isCompleted);
        resultIntent.putExtra("wontDo", wontDo);
        resultIntent.putExtra("isEdit", isEdit);
        resultIntent.putExtra("position", position);
        resultIntent.putExtra(EXTRA_IMAGE_PATH, currentImageFile);

        setResult(RESULT_OK, resultIntent);
        finish();
    }

    /**
     * Gọi khi người dùng nhấn Back/X (không nhấn Save) -> xóa tất cả files
     * được chọn trong session này, không xóa file gốc.
     */
    private void cleanupOrphanFiles() {
        for (String f : sessionFiles) {
            if (!f.equals(originalImageFile)) {
                storage.deleteImage(f);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        CategoryOption option = (CategoryOption) spinnerCategory.getSelectedItem();
        outState.putBoolean("isEdit", isEdit);
        outState.putInt("position", position);
        outState.putString("id", id);
        outState.putBoolean("isCompleted", isCompleted);
        outState.putBoolean("wontDo", wontDo);
        outState.putString("selectedCategoryId", option == null ? selectedCategoryId : option.id);
        outState.putString("originalImageFile", originalImageFile);
        outState.putString("currentImageFile", currentImageFile);
        outState.putStringArrayList("sessionFiles", new ArrayList<>(sessionFiles));
    }

    /**
     * Dùng làm item cho Spinner chọn category
     */
    static class CategoryOption {
        final String id;
        final String name;

        CategoryOption(String id, String name) {
            this.id = id;
            this.name = name;
        }

        @NonNull
        @Override
        public String toString() {
            return name;
        }
    }
}
