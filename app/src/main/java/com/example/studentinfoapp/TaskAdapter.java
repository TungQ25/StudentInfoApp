package com.example.studentinfoapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class TaskAdapter extends ArrayAdapter<Task> {
    private Context context;
    private List<Task> taskList;

    public TaskAdapter(@NonNull Context context, List<Task> taskList) {
        super(context, 0, taskList);
        this.context = context;
        this.taskList = taskList;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_task, parent, false);
        }

        Task currentTask = taskList.get(position);

        TextView tvTitle = convertView.findViewById(R.id.tvTaskTitle);
        CheckBox cbCompleted = convertView.findViewById(R.id.cbTaskCompleted);

        tvTitle.setText(currentTask.getTitle());
        cbCompleted.setOnCheckedChangeListener(null); // Clear listener before setting status
        cbCompleted.setChecked(currentTask.isCompleted());

        cbCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
            currentTask.setCompleted(isChecked);
        });

        return convertView;
    }
}
