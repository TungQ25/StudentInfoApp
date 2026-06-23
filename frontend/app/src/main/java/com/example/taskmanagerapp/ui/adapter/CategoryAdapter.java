package com.example.taskmanagerapp.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.taskmanagerapp.R;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private final List<String> categories;
    private final OnCategoryClickListener listener;
    private String selectedCategory = "All";

    public interface OnCategoryClickListener {
        void onCategoryClick(String category);
    }

    public CategoryAdapter(List<String> categories, OnCategoryClickListener listener) {
        this.categories = categories;
        this.listener = listener;
    }

    public void setSelectedCategory(String selectedCategory) {
        this.selectedCategory = selectedCategory;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    // Dùng để lấy layout từ list_item_category và tạo ra ViewHolder (tái sử dụng view)
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        String category = categories.get(position);
        boolean isSelected = category.equals(selectedCategory);
        holder.itemView.setSelected(isSelected);
        holder.tvCategoryName.setText(category);
        holder.card.setCardBackgroundColor(ContextCompat.getColor(
                holder.itemView.getContext(),
                isSelected ? R.color.colorSurface : R.color.colorPrimary
        ));
        holder.card.setStrokeColor(ContextCompat.getColor(
                holder.itemView.getContext(),
                isSelected ? R.color.colorOutline : R.color.colorPrimary
        ));
        holder.card.setStrokeWidth(
                isSelected
                        ? (int) holder.itemView.getResources().getDisplayMetrics().density
                        : 0
        );
        holder.tvCategoryName.setTextColor(ContextCompat.getColor(
                holder.itemView.getContext(),
                isSelected ? R.color.colorOnSurfaceVariant : R.color.colorOnPrimary
        ));
        holder.itemView.setOnClickListener(v -> listener.onCategoryClick(category));
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView card;
        TextView tvCategoryName;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            card = (MaterialCardView) itemView;
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
        }
    }
}
