package com.example.studentinfoapp;

import android.os.Handler;
import android.os.Looper;
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

    public interface OnTaskClickListener {
        void onTaskClick(Task task, int position);
        void onTaskDoubleClick(Task task, int position);
        void onTaskLongClick(Task task, int position);
        void onStatusChanged(Task task, boolean isCompleted);
    }

    public TaskAdapter(OnTaskClickListener listener) {
        super(new DiffUtil.ItemCallback<Task>() {
            @Override
            public boolean areItemsTheSame(@NonNull Task oldItem, @NonNull Task newItem) {
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
        
        private long lastClickTime = 0;
        private final Handler handler = new Handler(Looper.getMainLooper());
        private Runnable pendingClick;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            root = itemView;
            tvTitle = itemView.findViewById(R.id.taskTitle);
            tvDate = itemView.findViewById(R.id.taskDate);
            cbCompleted = itemView.findViewById(R.id.checkComplete);
            ivPriority = itemView.findViewById(R.id.priorityIcon);
        }

        public void bind(Task task, OnTaskClickListener listener) {
            tvTitle.setText(task.getTitle());
            tvDate.setText(task.getDeadline());
            
            if (task.isSelected()) {
                root.setBackgroundColor(0x332F2F2F);
            } else {
                root.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            }

            cbCompleted.setOnCheckedChangeListener(null);
            cbCompleted.setChecked(task.isCompleted());
            cbCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
                listener.onStatusChanged(task, isChecked);
            });

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

            root.setOnClickListener(v -> {
                long currentTime = System.currentTimeMillis();
                int position = getBindingAdapterPosition();
                
                if (currentTime - lastClickTime < 300) {
                    // Double Click detected: Hủy click đơn đang chờ và chạy double click
                    if (pendingClick != null) {
                        handler.removeCallbacks(pendingClick);
                        pendingClick = null;
                    }
                    listener.onTaskDoubleClick(task, position);
                } else {
                    // Click đơn: Chờ 300ms xem có click thứ 2 không
                    pendingClick = () -> {
                        listener.onTaskClick(task, position);
                        pendingClick = null;
                    };
                    handler.postDelayed(pendingClick, 300);
                }
                lastClickTime = currentTime;
            });

            root.setOnLongClickListener(v -> {
                listener.onTaskLongClick(task, getBindingAdapterPosition());
                return true;
            });
        }
    }
}
