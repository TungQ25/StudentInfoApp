package com.example.taskmanagerapp.ui.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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

import com.example.taskmanagerapp.R;
import com.example.taskmanagerapp.utils.ImageStorageHelper;

public class AddTaskActivity extends AppCompatActivity {
    private static final String TAG = "AddTaskLifecycle";
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

    ImageStorageHelper storage;

    /** Tên file ảnh khi vào activity (null nếu không có ảnh). */
    String originalImageFile = null;
    /** File ảnh hiện tại được hiển thị / sẽ được lưu vào task. */
    String currentImageFile = null;
    /** Files được chọn trong session này cần được xóa nếu người dùng thoát. */
    final java.util.List<String> sessionFiles = new java.util.ArrayList<>();

    ActivityResultLauncher<PickVisualMediaRequest> pickImageLauncher;

    String[] categories = {"Homework", "Project", "Exam"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Activity Created");
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_task);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        storage = new ImageStorageHelper(this);

        edtTitle = findViewById(R.id.edtTitle);
        edtDescription = findViewById(R.id.edtDescription);
        edtDeadline = findViewById(R.id.edtDeadline);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        rgPriority = findViewById(R.id.rgPriority);
        btnSave = findViewById(R.id.btnSave);
        btnPickAttachment = findViewById(R.id.btnPickAttachment);
        btnRemoveAttachment = findViewById(R.id.btnRemoveAttachment);
        ivAttachment = findViewById(R.id.ivAttachment);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        if (savedInstanceState != null) {
            isEdit = savedInstanceState.getBoolean("isEdit");
            position = savedInstanceState.getInt("position");
            id = savedInstanceState.getString("id");
            isCompleted = savedInstanceState.getBoolean("isCompleted");
            originalImageFile = savedInstanceState.getString("originalImageFile");
            currentImageFile = savedInstanceState.getString("currentImageFile");
            java.util.ArrayList<String> sf = savedInstanceState.getStringArrayList("sessionFiles");
            if (sf != null) sessionFiles.addAll(sf);
            updateAttachmentUi();
        } else {
            Intent intent = getIntent();
            if (intent != null) {
                isEdit = intent.getBooleanExtra("isEdit", false);
                position = intent.getIntExtra("position", -1);
                id = intent.getStringExtra("id");

                if (isEdit) {
                    edtTitle.setText(intent.getStringExtra("title"));
                    edtDescription.setText(intent.getStringExtra("description"));
                    edtDeadline.setText(intent.getStringExtra("deadline"));
                    isCompleted = intent.getBooleanExtra("completed", false);

                    String category = intent.getStringExtra("category");
                    for (int i = 0; i < categories.length; i++) {
                        if (categories[i].equals(category)) {
                            spinnerCategory.setSelection(i);
                            break;
                        }
                    }

                    String priority = intent.getStringExtra("priority");
                    if ("Low".equals(priority)) rgPriority.check(R.id.rbLow);
                    else if ("Medium".equals(priority)) rgPriority.check(R.id.rbMedium);
                    else if ("High".equals(priority)) rgPriority.check(R.id.rbHigh);

                    originalImageFile = intent.getStringExtra(EXTRA_IMAGE_PATH);
                    currentImageFile = originalImageFile;
                    updateAttachmentUi();
                }
            }
        }

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(), this::onPickedImage);

        btnPickAttachment.setOnClickListener(v -> pickImageLauncher.launch(
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()));

        btnRemoveAttachment.setOnClickListener(v -> removeCurrentAttachment());

        btnSave.setOnClickListener(v -> {
            saveTask();
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                cleanupOrphanFiles();
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
            }
        });
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
        String category = spinnerCategory.getSelectedItem().toString();

        int selectedPriorityId = rgPriority.getCheckedRadioButtonId();
        RadioButton rbSelected = findViewById(selectedPriorityId);
        String priority = rbSelected.getText().toString();

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
        resultIntent.putExtra("category", category);
        resultIntent.putExtra("deadline", deadline);
        resultIntent.putExtra("priority", priority);
        resultIntent.putExtra("completed", isCompleted);
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
        outState.putBoolean("isEdit", isEdit);
        outState.putInt("position", position);
        outState.putString("id", id);
        outState.putBoolean("isCompleted", isCompleted);
        outState.putString("originalImageFile", originalImageFile);
        outState.putString("currentImageFile", currentImageFile);
        outState.putStringArrayList("sessionFiles", new java.util.ArrayList<>(sessionFiles));
    }
}
