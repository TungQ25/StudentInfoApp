package com.example.studentinfoapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class TaskDetailActivity extends AppCompatActivity {

    TextView tvTitle, tvDescription, tvCategory, tvPriority, tvDeadline, tvStatus;
    Button btnEdit, btnDelete;

    String id, title, description, category, priority, deadline;
    boolean isCompleted;
    int position;
    ActivityResultLauncher<Intent> editLauncher;

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

        tvTitle = findViewById(R.id.tvTitle);
        tvDescription = findViewById(R.id.tvDescription);
        tvCategory = findViewById(R.id.tvCategory);
        tvPriority = findViewById(R.id.tvPriority);
        tvDeadline = findViewById(R.id.tvDeadline);
        tvStatus = findViewById(R.id.tvStatus);
        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);

        Intent intent = getIntent();
        id = intent.getStringExtra("id");
        title = intent.getStringExtra("title");
        description = intent.getStringExtra("description");
        category = intent.getStringExtra("category");
        priority = intent.getStringExtra("priority");
        deadline = intent.getStringExtra("deadline");
        isCompleted = intent.getBooleanExtra("completed", false);
        position = intent.getIntExtra("position", -1);

        updateUI();

        editLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();

                        title = data.getStringExtra("title");
                        description = data.getStringExtra("description");
                        category = data.getStringExtra("category");
                        priority = data.getStringExtra("priority");
                        deadline = data.getStringExtra("deadline");
                        isCompleted = data.getBooleanExtra("completed", false);

                        updateUI();

                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("id", id);
                        resultIntent.putExtra("title", title);
                        resultIntent.putExtra("description", description);
                        resultIntent.putExtra("category", category);
                        resultIntent.putExtra("priority", priority);
                        resultIntent.putExtra("deadline", deadline);
                        resultIntent.putExtra("completed", isCompleted);
                        resultIntent.putExtra("position", position);
                        resultIntent.putExtra("updated", true);
                        setResult(RESULT_OK, resultIntent);
                    }
                }
        );

        btnEdit.setOnClickListener(v -> {
            Intent editIntent = new Intent(TaskDetailActivity.this, AddTaskActivity.class);
            editIntent.putExtra("id", id);
            editIntent.putExtra("title", title);
            editIntent.putExtra("description", description);
            editIntent.putExtra("category", category);
            editIntent.putExtra("priority", priority);
            editIntent.putExtra("deadline", deadline);
            editIntent.putExtra("completed", isCompleted);
            editIntent.putExtra("position", position);
            editIntent.putExtra("isEdit", true);
            editLauncher.launch(editIntent);
        });

        btnDelete.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("delete_position", position);
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }

    private void updateUI() {
        tvTitle.setText("Title: " + title);
        tvDescription.setText("Description: " + description);
        tvCategory.setText("Category: " + category);
        tvPriority.setText("Priority: " + priority);
        tvDeadline.setText("Deadline: " + deadline);
        tvStatus.setText("Status: " + (isCompleted ? "Completed" : "Pending"));
    }
}