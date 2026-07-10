package com.example.taskmanagerapp.ui.adapter;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.taskmanagerapp.R;
import com.example.taskmanagerapp.data.model.Category;
import com.example.taskmanagerapp.data.model.Task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TaskAdapter extends ListAdapter<Task, TaskAdapter.TaskViewHolder> {

    private static final int COLOR_CARD_DEFAULT = Color.rgb(21, 26, 31);
    private static final int COLOR_CARD_STROKE = Color.rgb(37, 42, 48);
    private static final int COLOR_TEXT_TITLE = Color.rgb(242, 244, 247);
    private static final int COLOR_TEXT_MUTED = Color.rgb(143, 150, 158);
    private static final int COLOR_HIGH = Color.rgb(255, 92, 92);
    private static final int COLOR_MEDIUM = Color.rgb(255, 184, 77);
    private static final int COLOR_LOW = Color.rgb(38, 161, 218);
    private static final int COLOR_NONE = Color.rgb(93, 101, 112);

    private final OnTaskClickListener listener;
    private final Map<String, Category> categoryById = new HashMap<>();
    private String selectedTaskId;

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

    public void setCategories(List<Category> categories) {
        categoryById.clear();
        if (categories != null) {
            for (Category category : categories) {
                if (category != null) {
                    categoryById.put(category.getId(), category);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void setSelectedTaskId(String taskId) {
        if (sameId(selectedTaskId, taskId)) {
            return;
        }
        String oldTaskId = selectedTaskId;
        selectedTaskId = taskId;
        notifyTaskChanged(oldTaskId);
        notifyTaskChanged(selectedTaskId);
    }

    public void clearSelectedTaskId() {
        setSelectedTaskId(null);
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

    private void notifyTaskChanged(String taskId) {
        int position = findPositionById(taskId);
        if (position >= 0) {
            notifyItemChanged(position);
        }
    }

    private int findPositionById(String taskId) {
        if (taskId == null) {
            return -1;
        }
        List<Task> tasks = getCurrentList();
        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);
            if (task != null && taskId.equals(task.getId())) {
                return i;
            }
        }
        return -1;
    }

    private boolean sameId(String first, String second) {
        return first == null ? second == null : first.equals(second);
    }

    class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDeadline, tvCategory, tvCategoryIcon;
        CheckBox cbCompleted;
        View root, card, accent;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            root = itemView;
            card = itemView.findViewById(R.id.taskCard);
            accent = itemView.findViewById(R.id.priorityAccent);
            tvTitle = itemView.findViewById(R.id.taskTitle);
            tvDeadline = itemView.findViewById(R.id.taskDate);
            tvCategory = itemView.findViewById(R.id.taskCategory);
            tvCategoryIcon = itemView.findViewById(R.id.taskCategoryIcon);
            cbCompleted = itemView.findViewById(R.id.checkComplete);
        }

        public void bind(Task task, OnTaskClickListener listener) {
            boolean selected = task.isSelected() || sameId(task.getId(), selectedTaskId);
            int priorityColor = priorityColor(task.getPriority());

            tvTitle.setText(task.getTitle());
            tvDeadline.setText(displayDeadline(task.getDeadline()));
            tvCategory.setText(categoryName(task.getCategoryId()));
            tvCategoryIcon.setText(categoryIcon(task.getCategoryId()));

            applyTaskStyle(task, selected, priorityColor);

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

            root.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Task current = getItem(position);
                    setSelectedTaskId(current.getId());
                    listener.onTaskClick(current, position);
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

        private void applyTaskStyle(Task task, boolean selected, int priorityColor) {
            int fillColor = selected ? selectedFillColor(task.getPriority()) : COLOR_CARD_DEFAULT;
            GradientDrawable cardBackground = new GradientDrawable();
            cardBackground.setShape(GradientDrawable.RECTANGLE);
            cardBackground.setColor(fillColor);
            cardBackground.setCornerRadius(dp(10));
            int strokeColor = priorityColor == COLOR_NONE
                    ? COLOR_CARD_STROKE
                    : blend(priorityColor, COLOR_CARD_STROKE, selected ? 0.12f : 0.38f);
            cardBackground.setStroke(dp(1), strokeColor);
            card.setBackground(cardBackground);

            GradientDrawable accentBackground = new GradientDrawable();
            accentBackground.setShape(GradientDrawable.RECTANGLE);
            accentBackground.setColor(priorityColor);
            accentBackground.setCornerRadius(dp(2));
            accent.setBackground(accentBackground);
            accent.setAlpha(selected ? 1f : 0.42f);
            accent.setVisibility(priorityColor == COLOR_NONE ? View.INVISIBLE : View.VISIBLE);

            int metadataColor = priorityColor == COLOR_NONE ? COLOR_TEXT_MUTED : priorityColor;
            tvTitle.setTextColor(COLOR_TEXT_TITLE);
            tvTitle.setTypeface(Typeface.DEFAULT, selected ? Typeface.BOLD : Typeface.NORMAL);
            tvCategory.setTextColor(metadataColor);
            tvCategoryIcon.setTextColor(metadataColor);
            tvDeadline.setTextColor(metadataColor);
            cbCompleted.setBackgroundTintList(ColorStateList.valueOf(metadataColor));
            root.setAlpha(task.isCompleted() ? 0.42f : 1f);
        }

        private int priorityColor(String priority) {
            String normalized = normalizePriority(priority);
            if ("high".equals(normalized)) {
                return COLOR_HIGH;
            }
            if ("medium".equals(normalized)) {
                return COLOR_MEDIUM;
            }
            if ("low".equals(normalized)) {
                return COLOR_LOW;
            }
            return COLOR_NONE;
        }

        private int selectedFillColor(String priority) {
            String normalized = normalizePriority(priority);
            if ("high".equals(normalized)) {
                return Color.rgb(38, 22, 24);
            }
            if ("medium".equals(normalized)) {
                return Color.rgb(38, 31, 18);
            }
            if ("low".equals(normalized)) {
                return Color.rgb(18, 36, 35);
            }
            return Color.rgb(26, 30, 35);
        }

        private String normalizePriority(String priority) {
            return priority == null ? "" : priority.trim().toLowerCase(Locale.US);
        }

        private String categoryName(String categoryId) {
            if (categoryId == null || categoryId.trim().isEmpty()) {
                return "Inbox";
            }
            Category category = categoryById.get(categoryId);
            return category == null ? "Inbox" : category.getName();
        }

        private String categoryIcon(String categoryId) {
            if (categoryId == null || categoryId.trim().isEmpty()) {
                return "\uD83D\uDCE5";
            }
            Category category = categoryById.get(categoryId);
            if (category == null || category.getIcon() == null || category.getIcon().trim().isEmpty()) {
                return "\uD83D\uDCCB";
            }
            String icon = category.getIcon().trim();
            return "#".equals(icon) ? "\uD83D\uDCCB" : icon;
        }

        private String displayDeadline(String deadline) {
            return deadline == null || deadline.trim().isEmpty() ? "" : deadline.trim();
        }

        private int blend(int first, int second, float ratio) {
            float inverse = 1f - ratio;
            return Color.rgb(
                    Math.round(Color.red(first) * inverse + Color.red(second) * ratio),
                    Math.round(Color.green(first) * inverse + Color.green(second) * ratio),
                    Math.round(Color.blue(first) * inverse + Color.blue(second) * ratio)
            );
        }

        private int dp(int value) {
            return Math.round(value * itemView.getResources().getDisplayMetrics().density);
        }
    }
}
