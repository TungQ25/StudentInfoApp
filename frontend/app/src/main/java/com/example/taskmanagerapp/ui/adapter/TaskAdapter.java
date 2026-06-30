package com.example.taskmanagerapp.ui.adapter;

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

import com.example.taskmanagerapp.R;
import com.example.taskmanagerapp.data.model.Task;

public class TaskAdapter extends ListAdapter<Task, TaskAdapter.TaskViewHolder> {

    private final OnTaskClickListener listener;

    public interface OnTaskClickListener {
        void onTaskClick(Task task, int position);
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
            root.setAlpha(task.isCompleted() ? 0.42f : 1f);

            cbCompleted.setOnCheckedChangeListener(null);
            cbCompleted.setChecked(task.isCompleted());
            cbCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Task current = getItem(position);
                    current.setCompleted(isChecked);
                    listener.onStatusChanged(current, isChecked);
                }
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
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onTaskClick(getItem(position), position);
                }
            });

            root.setOnLongClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onTaskLongClick(getItem(position), position);
                }
                return true;
            });
        }
    }
}
