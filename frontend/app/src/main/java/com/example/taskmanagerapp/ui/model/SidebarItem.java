package com.example.taskmanagerapp.ui.model;

import com.example.taskmanagerapp.data.model.Category;

public class SidebarItem {
    public enum Type {
        USER_HEADER,
        SECTION_TITLE,
        PINNED_CATEGORY,
        SMART_FILTER,
        CATEGORY,
        SYSTEM_FILTER,
        ADD_BUTTON
    }

    private final Type type;
    private final String id;
    private final String title;
    private final String icon;
    private final int count;
    private final boolean showCount;
    private final boolean selected;
    private final Category category;
    private final SmartFilter smartFilter;
    private final SystemFilter systemFilter;

    public SidebarItem(Type type, String id, String title, String icon, int count, boolean showCount, boolean selected, Category category, SmartFilter smartFilter, SystemFilter systemFilter) {
        this.type = type;
        this.id = id;
        this.title = title;
        this.icon = icon;
        this.count = count;
        this.showCount = showCount;
        this.selected = selected;
        this.category = category;
        this.smartFilter = smartFilter;
        this.systemFilter = systemFilter;
    }

    public static SidebarItem header(String title) {
        return new SidebarItem(Type.USER_HEADER, "header", title, "", 0, false, false, null, null, null);
    }

    public static SidebarItem section(String title) {
        return new SidebarItem(Type.SECTION_TITLE, "section:" + title, title, "", 0, false, false, null, null, null);
    }

    public static SidebarItem add() {
        return new SidebarItem(Type.ADD_BUTTON, "add", "Add", "+", 0, false, false, null, null, null);
    }

    public Type getType() { return type; }
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getIcon() { return icon; }
    public int getCount() { return count; }
    public boolean isShowCount() { return showCount; }
    public boolean isSelected() { return selected; }
    public Category getCategory() { return category; }
    public SmartFilter getSmartFilter() { return smartFilter; }
    public SystemFilter getSystemFilter() { return systemFilter; }
}
