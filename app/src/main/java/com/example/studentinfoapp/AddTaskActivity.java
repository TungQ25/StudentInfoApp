package com.example.studentinfoapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class AddTaskActivity extends AppCompatActivity {
    private static final String TAG = "AddTaskLifecycle";

    EditText edtTitle, edtDescription, edtDeadline;
    Spinner spinnerCategory;
    RadioGroup rgPriority;
    Button btnSave;

    boolean isEdit = false;
    int position = -1;

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

        edtTitle = findViewById(R.id.edtTitle);
        edtDescription = findViewById(R.id.edtDescription);
        edtDeadline = findViewById(R.id.edtDeadline);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        rgPriority = findViewById(R.id.rgPriority);
        btnSave = findViewById(R.id.btnSave);

        // Setup Spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        if (savedInstanceState != null) {
            isEdit = savedInstanceState.getBoolean("isEdit");
            position = savedInstanceState.getInt("position");
            Log.d(TAG, "onCreate: Restored from savedInstanceState");
        } else {
            Intent intent = getIntent();
            if (intent != null) {
                isEdit = intent.getBooleanExtra("isEdit", false);
                position = intent.getIntExtra("position", -1);

                if (isEdit) {
                    edtTitle.setText(intent.getStringExtra("title"));
                    edtDescription.setText(intent.getStringExtra("description"));
                    edtDeadline.setText(intent.getStringExtra("deadline"));

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
                }
            }
        }

        btnSave.setOnClickListener(v -> {
            if (validateData()) {
                saveTask();
            }
        });
    }

    private boolean validateData() {
        if (TextUtils.isEmpty(edtTitle.getText().toString().trim())) {
            edtTitle.setError("Task name is required");
            return false;
        }
        if (TextUtils.isEmpty(edtDeadline.getText().toString().trim())) {
            edtDeadline.setError("Due date is required");
            return false;
        }
        return true;
    }

    private void saveTask() {
        String title = edtTitle.getText().toString().trim();
        String description = edtDescription.getText().toString().trim();
        String deadline = edtDeadline.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();

        int selectedPriorityId = rgPriority.getCheckedRadioButtonId();
        RadioButton rbSelected = findViewById(selectedPriorityId);
        String priority = rbSelected.getText().toString();

        Intent resultIntent = new Intent();
        resultIntent.putExtra("title", title);
        resultIntent.putExtra("description", description);
        resultIntent.putExtra("category", category);
        resultIntent.putExtra("deadline", deadline);
        resultIntent.putExtra("priority", priority);
        resultIntent.putExtra("isEdit", isEdit);
        resultIntent.putExtra("position", position);

        setResult(RESULT_OK, resultIntent);
        finish();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("isEdit", isEdit);
        outState.putInt("position", position);
    }

    @Override
    protected void onStart() { super.onStart(); Log.d(TAG, "onStart"); }
    @Override
    protected void onResume() { super.onResume(); Log.d(TAG, "onResume"); }
    @Override
    protected void onPause() { super.onPause(); Log.d(TAG, "onPause"); }
    @Override
    protected void onStop() { super.onStop(); Log.d(TAG, "onStop"); }
    @Override
    protected void onDestroy() { super.onDestroy(); Log.d(TAG, "onDestroy"); }
}