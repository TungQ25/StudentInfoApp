package com.example.taskmanagerapp.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.taskmanagerapp.R;
import com.example.taskmanagerapp.data.model.Category;
import com.example.taskmanagerapp.ui.model.SidebarItem;
import com.example.taskmanagerapp.ui.model.SmartFilter;
import com.example.taskmanagerapp.ui.model.SystemFilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SidebarAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_HEADER = 0;
    private static final int VIEW_SECTION = 1;
    private static final int VIEW_ROW = 2;

    private final List<SidebarItem> items = new ArrayList<>();
    private final OnSidebarActionListener listener;

    public interface OnSidebarActionListener {
        void onSidebarItemClick(SidebarItem item);
        void onSidebarItemLongClick(SidebarItem item, View anchor);
        void onSidebarOverflowClick(SidebarItem item, View anchor);
        void onSidebarSettingsClick();
        void onSidebarNotificationClick();
    }

    public SidebarAdapter(OnSidebarActionListener listener) {
        this.listener = listener;
    }

    public void submitItems(List<SidebarItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public SidebarItem getItem(int position) {
        if (position < 0 || position >= items.size()) return null;
        return items.get(position);
    }

    public boolean canMove(int position) {
        SidebarItem item = getItem(position);
        return isMovable(item);
    }

    public boolean moveItem(int fromPosition, int toPosition) {
        if (!canMove(fromPosition) || !canMove(toPosition)) return false;
        if (items.get(fromPosition).getType() != items.get(toPosition).getType()) return false;
        Collections.swap(items, fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
        return true;
    }

    private boolean isMovable(SidebarItem item) {
        if (item == null) return false;
        return item.getType() == SidebarItem.Type.SMART_FILTER
                || item.getType() == SidebarItem.Type.CATEGORY
                || item.getType() == SidebarItem.Type.SYSTEM_FILTER;
    }

    public List<Category> getOrderedMainCategories() {
        List<Category> categories = new ArrayList<>();
        for (SidebarItem item : items) {
            if (item.getType() == SidebarItem.Type.CATEGORY && item.getCategory() != null) {
                categories.add(item.getCategory());
            }
        }
        return categories;
    }

    public List<SmartFilter> getOrderedSmartFilters() {
        List<SmartFilter> filters = new ArrayList<>();
        for (SidebarItem item : items) {
            if (item.getType() == SidebarItem.Type.SMART_FILTER && item.getSmartFilter() != null) {
                filters.add(item.getSmartFilter());
            }
        }
        return filters;
    }

    public List<SystemFilter> getOrderedSystemFilters() {
        List<SystemFilter> filters = new ArrayList<>();
        for (SidebarItem item : items) {
            if (item.getType() == SidebarItem.Type.SYSTEM_FILTER && item.getSystemFilter() != null) {
                filters.add(item.getSystemFilter());
            }
        }
        return filters;
    }

    @Override
    public int getItemViewType(int position) {
        SidebarItem item = items.get(position);
        if (item.getType() == SidebarItem.Type.USER_HEADER) return VIEW_HEADER;
        if (item.getType() == SidebarItem.Type.SECTION_TITLE) return VIEW_SECTION;
        return VIEW_ROW;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_HEADER) {
            return new HeaderViewHolder(inflater.inflate(R.layout.list_item_sidebar_header, parent, false));
        }
        if (viewType == VIEW_SECTION) {
            return new SectionViewHolder(inflater.inflate(R.layout.list_item_sidebar_section, parent, false));
        }
        return new RowViewHolder(inflater.inflate(R.layout.list_item_sidebar, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        SidebarItem item = items.get(position);
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind(item);
        } else if (holder instanceof SectionViewHolder) {
            ((SectionViewHolder) holder).bind(item);
        } else if (holder instanceof RowViewHolder) {
            ((RowViewHolder) holder).bind(item);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView avatar;
        TextView notify;
        TextView settings;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvHeaderTitle);
            avatar = itemView.findViewById(R.id.tvHeaderAvatar);
            notify = itemView.findViewById(R.id.btnHeaderNotify);
            settings = itemView.findViewById(R.id.btnHeaderSettings);
        }

        void bind(SidebarItem item) {
            title.setText(item.getTitle());
            String name = item.getTitle() == null || item.getTitle().isEmpty() ? "U" : item.getTitle().substring(0, 1).toUpperCase();
            avatar.setText(name);
            notify.setOnClickListener(v -> listener.onSidebarNotificationClick());
            settings.setOnClickListener(v -> listener.onSidebarSettingsClick());
        }
    }

    static class SectionViewHolder extends RecyclerView.ViewHolder {
        TextView title;

        SectionViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvSidebarSection);
        }

        void bind(SidebarItem item) {
            title.setText(item.getTitle());
        }
    }

    class RowViewHolder extends RecyclerView.ViewHolder {
        TextView icon;
        TextView title;
        TextView count;
        TextView more;

        RowViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.tvSidebarIcon);
            title = itemView.findViewById(R.id.tvSidebarTitle);
            count = itemView.findViewById(R.id.tvSidebarCount);
            more = itemView.findViewById(R.id.btnSidebarMore);
        }

        void bind(SidebarItem item) {
            itemView.setBackgroundResource(item.isSelected() ? R.drawable.bg_sidebar_selected : 0);
            icon.setText(item.getIcon() == null || item.getIcon().isEmpty() ? "#" : item.getIcon());
            title.setText(item.getTitle());
            if (item.isShowCount()) {
                count.setVisibility(View.VISIBLE);
                count.setText(String.valueOf(item.getCount()));
            } else {
                count.setVisibility(View.GONE);
            }

            boolean hasMenu = item.getType() == SidebarItem.Type.CATEGORY || item.getType() == SidebarItem.Type.PINNED_CATEGORY;
            more.setVisibility(hasMenu ? View.VISIBLE : View.GONE);
            more.setOnClickListener(v -> listener.onSidebarOverflowClick(item, more));
            itemView.setOnClickListener(v -> listener.onSidebarItemClick(item));
            itemView.setOnLongClickListener(v -> {
                listener.onSidebarItemLongClick(item, itemView);
                return true;
            });
        }
    }
}
