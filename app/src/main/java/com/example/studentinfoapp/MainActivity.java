package com.example.studentinfoapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity_Lifecycle";

    ListView listView;
    Button btnAddTask;
    ArrayList<Task> taskList;
    TaskAdapter adapter;

    ActivityResultLauncher<Intent> addTaskLauncher;
    ActivityResultLauncher<Intent> detailLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Activity Created");
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        listView = findViewById(R.id.listView);
        btnAddTask = findViewById(R.id.btnAddTask);

        if (savedInstanceState != null) {
            taskList = (ArrayList<Task>) savedInstanceState.getSerializable("taskList");
            if (taskList == null) {
                taskList = new ArrayList<>();
            }
        } else {
            taskList = new ArrayList<>();
        }

        adapter = new TaskAdapter(this, taskList);
        listView.setAdapter(adapter);

        // Khởi tạo StartActivityForResult để xử lý kết quả trả về từ AddTaskActivity
        addTaskLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();

                        String title = data.getStringExtra("title");
                        String description = data.getStringExtra("description");
                        String category = data.getStringExtra("category");
                        String deadline = data.getStringExtra("deadline");
                        boolean completed = data.getBooleanExtra("completed", false);
                        String priority = data.getStringExtra("priority");
                        
                        boolean isEdit = data.getBooleanExtra("isEdit", false);
                        int position = data.getIntExtra("position", -1);

                        Task task = new Task(title, description, category, deadline, completed, priority);

                        if (isEdit && position != -1) {
                            taskList.set(position, task);
                        } else {
                            taskList.add(task);
                            Toast.makeText(MainActivity.this, "Task added!", Toast.LENGTH_SHORT).show();
                        }

                        adapter.notifyDataSetChanged();
                    }
                }
        );

        // Khởi tạo StartActivityForResult để xử lý kết quả trả về từ TaskDetailActivity
        detailLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();

                        int deletePosition = data.getIntExtra("delete_position", -1);
                        boolean updated = data.getBooleanExtra("updated", false);

                        if (deletePosition != -1) {
                            taskList.remove(deletePosition);
                            adapter.notifyDataSetChanged();
                        } else if (updated) {
                            int position = data.getIntExtra("position", -1);
                            String title = data.getStringExtra("title");
                            String description = data.getStringExtra("description");
                            String category = data.getStringExtra("category");
                            String deadline = data.getStringExtra("deadline");
                            boolean completed = data.getBooleanExtra("completed", false);
                            String priority = data.getStringExtra("priority");

                            if (position != -1) {
                                taskList.set(position, new Task(title, description, category, deadline, completed, priority));
                                adapter.notifyDataSetChanged();
                            }
                        }
                    }
                }
        );

        btnAddTask.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddTaskActivity.class);
            addTaskLauncher.launch(intent);
        });

        // Xử lý sự kiện khi item trong ListView được nhấn
        listView.setOnItemClickListener((parent, view, position, id) -> {
            Task task = taskList.get(position);
            Intent intent = new Intent(MainActivity.this, TaskDetailActivity.class);
            intent.putExtra("title", task.getTitle());
            intent.putExtra("description", task.getDescription());
            intent.putExtra("category", task.getCategory());
            intent.putExtra("deadline", task.getDeadline());
            intent.putExtra("completed", task.isCompleted());
            intent.putExtra("priority", task.getPriority());
            intent.putExtra("position", position);
            detailLauncher.launch(intent);
        });
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
    @Override
    protected void onRestart() { super.onRestart(); Log.d(TAG, "onRestart"); }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Lưu lại danh sách taskList
        outState.putSerializable("taskList", taskList);
        Log.d(TAG, "onSaveInstanceState: Data saved");
    }
}