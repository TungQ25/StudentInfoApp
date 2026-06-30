package com.example.taskmanagerapp.ui.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.taskmanagerapp.R;
import com.example.taskmanagerapp.utils.ImageStorageHelper;

public class TaskDetailActivity extends AppCompatActivity {

    TextView tvTitle, tvDescription, tvCategory, tvPriority, tvDeadline, tvStatus;
    ImageView ivAttachment;
    Button btnEdit, btnDelete, btnWontDo;

    String id, title, description, categoryId, categoryName, priority, deadline;
    String imagePath;
    boolean isCompleted;
    boolean wontDo;
    int position;
    ActivityResultLauncher<Intent> editLauncher;
    ImageStorageHelper storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_task_detail);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        storage = new ImageStorageHelper(this);
        tvTitle = findViewById(R.id.tvTitle);
        tvDescription = findViewById(R.id.tvDescription);
        tvCategory = findViewById(R.id.tvCategory);
        tvPriority = findViewById(R.id.tvPriority);
        tvDeadline = findViewById(R.id.tvDeadline);
        tvStatus = findViewById(R.id.tvStatus);
        ivAttachment = findViewById(R.id.ivAttachment);
        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);
        btnWontDo = findViewById(R.id.btnWontDo);

        readIntent(getIntent());
        updateUI();
        setupEditLauncher();
        setupActions();
    }

    private void readIntent(Intent intent) {
        id = intent.getStringExtra("id");
        title = intent.getStringExtra("title");
        description = intent.getStringExtra("description");
        categoryId = intent.getStringExtra("categoryId");
        categoryName = intent.getStringExtra("categoryName");
        priority = intent.getStringExtra("priority");
        deadline = intent.getStringExtra("deadline");
        isCompleted = intent.getBooleanExtra("completed", false);
        wontDo = intent.getBooleanExtra("wontDo", false);
        position = intent.getIntExtra("position", -1);
        imagePath = intent.getStringExtra(AddTaskActivity.EXTRA_IMAGE_PATH);
    }

    private void setupEditLauncher() {
        editLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Intent data = result.getData();
                title = data.getStringExtra("title");
                description = data.getStringExtra("description");
                categoryId = data.getStringExtra("categoryId");
                categoryName = null;
                priority = data.getStringExtra("priority");
                deadline = data.getStringExtra("deadline");
                isCompleted = data.getBooleanExtra("completed", false);
                wontDo = data.getBooleanExtra("wontDo", false);
                String newImagePath = data.getStringExtra(AddTaskActivity.EXTRA_IMAGE_PATH);
                if (imagePath != null && !imagePath.equals(newImagePath)) storage.deleteImage(imagePath);
                imagePath = newImagePath;
                updateUI();
                sendUpdatedResult();
                finish();
            }
        });
    }

    private void setupActions() {
        btnEdit.setOnClickListener(v -> {
            Intent editIntent = new Intent(TaskDetailActivity.this, AddTaskActivity.class);
            editIntent.putExtra("id", id);
            editIntent.putExtra("title", title);
            editIntent.putExtra("description", description);
            editIntent.putExtra("categoryId", categoryId);
            editIntent.putExtra("priority", priority);
            editIntent.putExtra("deadline", deadline);
            editIntent.putExtra("completed", isCompleted);
            editIntent.putExtra("wontDo", wontDo);
            editIntent.putExtra("position", position);
            editIntent.putExtra("isEdit", true);
            editIntent.putExtra(AddTaskActivity.EXTRA_IMAGE_PATH, imagePath);
            editLauncher.launch(editIntent);
        });

        btnWontDo.setOnClickListener(v -> {
            wontDo = !wontDo;
            if (wontDo) isCompleted = false;
            updateUI();
            sendUpdatedResult();
            finish();
        });

        btnDelete.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("id", id);
            resultIntent.putExtra("deleted", true);
            resultIntent.putExtra(AddTaskActivity.EXTRA_IMAGE_PATH, imagePath);
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }

    private void sendUpdatedResult() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("id", id);
        resultIntent.putExtra("title", title);
        resultIntent.putExtra("description", description);
        resultIntent.putExtra("categoryId", categoryId);
        resultIntent.putExtra("priority", priority);
        resultIntent.putExtra("deadline", deadline);
        resultIntent.putExtra("completed", isCompleted);
        resultIntent.putExtra("wontDo", wontDo);
        resultIntent.putExtra("position", position);
        resultIntent.putExtra(AddTaskActivity.EXTRA_IMAGE_PATH, imagePath);
        resultIntent.putExtra("updated", true);
        setResult(RESULT_OK, resultIntent);
    }

    private void updateUI() {
        String displayCategory = categoryName == null || categoryName.trim().isEmpty() ? "Inbox" : categoryName;
        String displayPriority = priority == null || priority.trim().isEmpty() ? "None" : priority;
        tvTitle.setText("Title: " + title);
        tvDescription.setText("Description: " + description);
        tvCategory.setText("Category: " + displayCategory);
        tvPriority.setText("Priority: " + displayPriority);
        tvDeadline.setText("Deadline: " + deadline);
        tvStatus.setText("Status: " + (wontDo ? "Wont Do" : (isCompleted ? "Completed" : "Pending")));
        btnWontDo.setText(wontDo ? "Undo Wont Do" : "Mark Wont Do");

        if (imagePath != null && !imagePath.isEmpty()) {
            Bitmap bm = storage.loadBitmapForView(imagePath, 1024, 1024);
            if (bm != null) {
                ivAttachment.setImageBitmap(bm);
                ivAttachment.setVisibility(View.VISIBLE);
            } else {
                ivAttachment.setVisibility(View.GONE);
            }
        } else {
            ivAttachment.setImageDrawable(null);
            ivAttachment.setVisibility(View.GONE);
        }
    }
}
