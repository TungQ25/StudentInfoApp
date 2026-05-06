package com.example.studentinfoapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class TaskAdapter extends ListAdapter<Task, TaskAdapter.TaskViewHolder> {

    private final OnTaskClickListener listener;

    // Interface để bắt sự kiện click và long click
    public interface OnTaskClickListener {
        void onTaskClick(Task task, int position);
        void onTaskLongClick(Task task, int position);
        void onStatusChanged(Task task, boolean isCompleted);
    }

    public TaskAdapter(OnTaskClickListener listener) {
        super(new DiffUtil.ItemCallback<Task>() {
            @Override
            public boolean areItemsTheSame(@NonNull Task oldItem, @NonNull Task newItem) {
                // Sử dụng getId() để so sánh chính xác các Task
                return oldItem.getId().equals(newItem.getId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull Task oldItem, @NonNull Task newItem) {
                return oldItem.equals(newItem);
            }
        });
        this.listener = listener;
    }

    @NonNull
    @Override
    // Dùng để lấy layout từ list_item_task và tạo ra ViewHolder (tái sử dụng view)
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = getItem(position);
        holder.bind(task, listener);
    }

    class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDate;
        CheckBox cbCompleted;
        ImageView ivPriority;
        View root;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            root = itemView;
            tvTitle = itemView.findViewById(R.id.taskTitle);
            tvDate = itemView.findViewById(R.id.taskDate);
            cbCompleted = itemView.findViewById(R.id.checkComplete);
            ivPriority = itemView.findViewById(R.id.priorityIcon);
        }

        // Bind data to views
        public void bind(Task task, OnTaskClickListener listener) {
            tvTitle.setText(task.getTitle());
            tvDate.setText(task.getDeadline());
            
            // Selection visual
            if (task.isSelected()) {
                root.setBackgroundColor(0x336200EE);
            } else {
                root.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            }

            cbCompleted.setOnCheckedChangeListener(null);
            cbCompleted.setChecked(task.isCompleted());
            cbCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
                listener.onStatusChanged(task, isChecked);
            });

            // Set priority icon based on priority string
            if ("High".equals(task.getPriority())) {
                ivPriority.setVisibility(View.VISIBLE);
                ivPriority.setImageResource(android.R.drawable.ic_notification_overlay);
                ivPriority.setColorFilter(android.graphics.Color.RED);
            } else if ("Medium".equals(task.getPriority())) {
                ivPriority.setVisibility(View.VISIBLE);
                ivPriority.setImageResource(android.R.drawable.ic_notification_overlay);
                ivPriority.setColorFilter(android.graphics.Color.YELLOW);
            } else {
                ivPriority.setVisibility(View.GONE);
            }

            root.setOnClickListener(v -> listener.onTaskClick(task, getAdapterPosition()));
            root.setOnLongClickListener(v -> {
                listener.onTaskLongClick(task, getAdapterPosition());
                return true;
            });
        }
    }
}
