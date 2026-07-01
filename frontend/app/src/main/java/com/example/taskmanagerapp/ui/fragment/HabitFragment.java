package com.example.taskmanagerapp.ui.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.taskmanagerapp.R;
import com.example.taskmanagerapp.data.model.Habit;
import com.example.taskmanagerapp.data.model.HabitCompletion;
import com.example.taskmanagerapp.data.repository.HabitRepository;
import com.example.taskmanagerapp.ui.activity.MainActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class HabitFragment extends Fragment implements MainActivity.HabitToolbarController {
    private static final String FILTER_ALL = "all";

    private final List<Habit> habits = new ArrayList<>();
    private final Set<String> completedPeriodKeys = new HashSet<>();
    private final Calendar selectedDate = Calendar.getInstance();
    private final SimpleDateFormat dayKeyFormat = new SimpleDateFormat("yyyyMMdd", Locale.US);
    private final SimpleDateFormat monthKeyFormat = new SimpleDateFormat("yyyyMM", Locale.US);

    private HabitRepository repository;
    private LinearLayout weekStrip;
    private LinearLayout filterStrip;
    private LinearLayout sectionList;
    private TextView emptyView;
    private String selectedFilter = FILTER_ALL;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_habit, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).showHabitToolbar(this);
        }

        repository = HabitRepository.getInstance(requireContext());
        weekStrip = view.findViewById(R.id.weekStrip);
        filterStrip = view.findViewById(R.id.habitFilterStrip);
        sectionList = view.findViewById(R.id.habitSectionList);
        emptyView = view.findViewById(R.id.tvHabitEmpty);
        FloatingActionButton addButton = view.findViewById(R.id.btnHabitAdd);

        render();
        observeHabitData();

        addButton.setOnClickListener(v -> showAddHabitDialog());
        repository.syncHabits();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).showHabitToolbar(this);
        }
    }

    /**
     * Tự động cập nhật khi habit thay đổi
     */
    private void observeHabitData() {
        repository.getHabitsLive().observe(getViewLifecycleOwner(), latestHabits -> {
            habits.clear();
            if (latestHabits != null) {
                habits.addAll(latestHabits);
            }
            renderSections();
        });
        repository.getCompletionsLive().observe(getViewLifecycleOwner(), completions -> {
            completedPeriodKeys.clear();
            if (completions != null) {
                for (HabitCompletion completion : completions) {
                    completedPeriodKeys.add(completionKey(completion.getHabitId(), completion.getPeriodKey()));
                }
            }
            renderSections();
        });
    }

    private void render() {
        renderWeekStrip();
        renderFilterStrip();
        renderSections();
    }

    /**
     * Hiển thị thanh chọn ngày trong tuần (7 ngày gần nhất)
     */
    private void renderWeekStrip() {
        weekStrip.removeAllViews();
        Calendar start = Calendar.getInstance();
        start.add(Calendar.DAY_OF_YEAR, -6); // Lấy ngày bắt đầu là 6 ngày trước hôm nay
        String selectedKey = dayKeyFormat.format(selectedDate.getTime());

        for (int i = 0; i < 7; i++) {
            Calendar day = (Calendar) start.clone();
            day.add(Calendar.DAY_OF_YEAR, i);

            // Tạo cột dọc dayLabel = tên thứ (Monday, Tuesday...) + dateLabel = số ngày
            LinearLayout dayColumn = new LinearLayout(requireContext());
            dayColumn.setGravity(Gravity.CENTER);
            dayColumn.setOrientation(LinearLayout.VERTICAL);
            dayColumn.setClickable(true);
            dayColumn.setFocusable(true);
            dayColumn.setOnClickListener(v -> {
                selectedDate.setTime(day.getTime()); // cập nhật lại ngày đang chọn thành ngày vừa bấm
                render(); // vẽ lại ngày
            });
            weekStrip.addView(dayColumn, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView dayLabel = new TextView(requireContext());
            dayLabel.setGravity(Gravity.CENTER);
            dayLabel.setText(new SimpleDateFormat("E", Locale.US).format(day.getTime()).substring(0, 1)); // Lấy ký tự đầu của thứ
            dayLabel.setTextColor(mutedColor());
            dayLabel.setTextSize(16f);
            dayColumn.addView(
                    dayLabel,
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    )
            );

            String dayKey = dayKeyFormat.format(day.getTime());
            boolean isSelected = selectedKey.equals(dayKey);

            // Kiểm tra đúng ngày đang chọn thì hiển thị chữ trắng nổi bật bằng màu cam
            TextView dateLabel = new TextView(requireContext());
            dateLabel.setGravity(Gravity.CENTER);
            dateLabel.setText(String.valueOf(day.get(Calendar.DAY_OF_MONTH)));
            dateLabel.setTextColor(isSelected ? onPrimaryColor() : onSurfaceColor());
            dateLabel.setTextSize(20f);
            dateLabel.setTypeface(Typeface.DEFAULT, isSelected ? Typeface.BOLD : Typeface.NORMAL);
            if (isSelected) dateLabel.setBackground(circleDrawable(accentColor()));

            LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(dp(54), dp(54));
            dateParams.topMargin = dp(14);
            dayColumn.addView(dateLabel, dateParams);
        }
    }

    /**
     * Tạo 4 nút lọc
     */
    private void renderFilterStrip() {
        filterStrip.removeAllViews();
        addFilterChip(FILTER_ALL, "All");
        addFilterChip(Habit.FREQUENCY_DAILY, "Daily");
        addFilterChip(Habit.FREQUENCY_WEEKLY, "Weekly");
        addFilterChip(Habit.FREQUENCY_MONTHLY, "Monthly");
    }

    private void addFilterChip(String filter, String label) {
        boolean selected = selectedFilter.equals(filter);
        TextView chip = new TextView(requireContext());
        chip.setGravity(Gravity.CENTER);
        chip.setText(label);
        chip.setTextColor(selected ? onPrimaryColor() : mutedColor());
        chip.setTextSize(14f);
        chip.setTypeface(Typeface.DEFAULT, selected ? Typeface.BOLD : Typeface.NORMAL);
        chip.setBackground(roundedDrawable(selected ? accentColor() : surfaceColor(), dp(22)));
        chip.setOnClickListener(v -> {
            selectedFilter = filter;
            renderFilterStrip();
            renderSections();
        });

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(42), 1f);
        params.setMargins(dp(4), 0, dp(4), 0);
        filterStrip.addView(chip, params);
    }

    private void renderSections() {
        if (sectionList == null || emptyView == null) {
            return;
        }
        sectionList.removeAllViews();
        Map<String, List<Habit>> groupedHabits = new LinkedHashMap<>();
        for (Habit habit : habits) {
            if (!FILTER_ALL.equals(selectedFilter) && !habit.getFrequency().equals(selectedFilter)) {
                continue;
            }
            String groupName = habit.getGroupName() == null || habit.getGroupName().trim().isEmpty()
                    ? "Habit"
                    : habit.getGroupName().trim();
            if (!groupedHabits.containsKey(groupName)) {
                groupedHabits.put(groupName, new ArrayList<>());
            }
            groupedHabits.get(groupName).add(habit);
        }

        emptyView.setVisibility(groupedHabits.isEmpty() ? View.VISIBLE : View.GONE);
        for (Map.Entry<String, List<Habit>> entry : groupedHabits.entrySet()) {
            sectionList.addView(createSectionCard(entry.getKey(), entry.getValue()));
        }
    }

    private View createSectionCard(String title, List<Habit> sectionHabits) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(14));
        card.setBackgroundResource(R.drawable.bg_habit_card);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.topMargin = dp(18);
        card.setLayoutParams(cardParams);

        LinearLayout header = new LinearLayout(requireContext());
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);
        card.addView(header, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView titleView = new TextView(requireContext());
        titleView.setText(title);
        titleView.setTextColor(onSurfaceColor());
        titleView.setTextSize(24f);
        titleView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        header.addView(titleView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView countView = new TextView(requireContext());
        countView.setText(String.valueOf(sectionHabits.size()));
        countView.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        countView.setTextColor(mutedColor());
        countView.setTextSize(18f);
        header.addView(countView, new LinearLayout.LayoutParams(dp(48), ViewGroup.LayoutParams.WRAP_CONTENT));

        for (Habit habit : sectionHabits) {
            card.addView(createHabitRow(habit));
        }
        return card;
    }

    private View createHabitRow(Habit habit) {
        boolean completed = completedPeriodKeys.contains(completionKey(habit.getId(), periodKey(habit)));

        LinearLayout row = new LinearLayout(requireContext());
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(16), 0, dp(2));
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(v -> repository.toggleCompletion(habit, periodKey(habit)));

        TextView icon = new TextView(requireContext());
        icon.setGravity(Gravity.CENTER);
        icon.setText(initials(habit.getTitle()));
        icon.setTextColor(onPrimaryColor());
        icon.setTextSize(12f);
        icon.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        icon.setBackground(circleDrawable(completed ? accentColor() : habit.getColor()));
        row.addView(icon, new LinearLayout.LayoutParams(dp(46), dp(46)));

        LinearLayout titleColumn = new LinearLayout(requireContext());
        titleColumn.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.setMarginStart(dp(14));
        row.addView(titleColumn, titleParams);

        TextView title = new TextView(requireContext());
        title.setText(habit.getTitle());
        title.setTextColor(completed ? accentColor() : onSurfaceColor());
        title.setTextSize(22f);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setSingleLine(false);
        titleColumn.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView frequency = new TextView(requireContext());
        frequency.setText(labelForFrequency(habit.getFrequency()));
        frequency.setTextColor(mutedColor());
        frequency.setTextSize(13f);
        titleColumn.addView(frequency, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout statColumn = new LinearLayout(requireContext());
        statColumn.setGravity(Gravity.END);
        statColumn.setOrientation(LinearLayout.VERTICAL);
        row.addView(statColumn, new LinearLayout.LayoutParams(dp(96), ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView totalDays = new TextView(requireContext());
        totalDays.setGravity(Gravity.END);
        totalDays.setText(String.valueOf(habit.getTotalDays()));
        totalDays.setTextColor(completed ? accentColor() : onSurfaceColor());
        totalDays.setTextSize(25f);
        totalDays.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        statColumn.addView(totalDays, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView label = new TextView(requireContext());
        label.setGravity(Gravity.END);
        label.setText(completed ? "Done" : "Total Days");
        label.setTextColor(mutedColor());
        label.setTextSize(14f);
        statColumn.addView(label, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        return row;
    }

    private void showAddHabitDialog() {
        Context context = requireContext();
        LinearLayout form = new LinearLayout(context);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(8), dp(20), 0);

        EditText titleInput = new EditText(context);
        titleInput.setHint("Tên habit");
        form.addView(titleInput, fieldParams());

        EditText groupInput = new EditText(context);
        groupInput.setHint("Nhóm, ví dụ: Morning");
        form.addView(groupInput, fieldParams());

        Spinner frequencyInput = new Spinner(context);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, new String[]{"Daily", "Weekly", "Monthly"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        frequencyInput.setAdapter(adapter);
        form.addView(frequencyInput, fieldParams());

        EditText totalInput = new EditText(context);
        totalInput.setHint("Total days ban đầu");
        totalInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        totalInput.setText("0");
        form.addView(totalInput, fieldParams());

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Thêm habit")
                .setView(form)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(accentColor());
            dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(mutedColor());
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
                String title = titleInput.getText().toString().trim();
                if (title.isEmpty()) {
                    titleInput.setError("Nhập tên habit");
                    return;
                }
                String group = groupInput.getText().toString().trim();
                int totalDays = parseTotalDays(totalInput.getText().toString());
                String frequency = frequencyFromLabel(frequencyInput.getSelectedItem().toString());
                repository.addHabit(title, group, frequency, totalDays, colorForIndex(habits.size()));
                dialog.dismiss();
            });
        });
        dialog.show();
    }

    private LinearLayout.LayoutParams fieldParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(8);
        return params;
    }

    private int parseTotalDays(String raw) {
        try {
            return Math.max(0, Integer.parseInt(raw.trim()));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private void showFilterMenu(View anchor) {
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        menu.getMenu().add("All");
        menu.getMenu().add("Daily");
        menu.getMenu().add("Weekly");
        menu.getMenu().add("Monthly");
        menu.setOnMenuItemClickListener(item -> {
            selectedFilter = frequencyFromLabel(item.getTitle().toString());
            renderFilterStrip();
            renderSections();
            return true;
        });
        menu.show();
    }

    private void showCompletionSummary() {
        int completed = 0;
        for (Habit habit : habits) {
            if (completedPeriodKeys.contains(completionKey(habit.getId(), periodKey(habit)))) {
                completed++;
            }
        }
        Toast.makeText(requireContext(), completed + "/" + habits.size() + " habits done", Toast.LENGTH_SHORT).show();
    }

    private String periodKey(Habit habit) {
        if (Habit.FREQUENCY_WEEKLY.equals(habit.getFrequency())) {
            Calendar date = (Calendar) selectedDate.clone();
            return "W:" + date.get(Calendar.YEAR) + ":" + date.get(Calendar.WEEK_OF_YEAR);
        }
        if (Habit.FREQUENCY_MONTHLY.equals(habit.getFrequency())) {
            return "M:" + monthKeyFormat.format(selectedDate.getTime());
        }
        return "D:" + dayKeyFormat.format(selectedDate.getTime());
    }

    private String completionKey(String habitId, String periodKey) {
        return habitId + "|" + periodKey;
    }

    private String frequencyFromLabel(String label) {
        if ("Weekly".equalsIgnoreCase(label)) {
            return Habit.FREQUENCY_WEEKLY;
        }
        if ("Monthly".equalsIgnoreCase(label)) {
            return Habit.FREQUENCY_MONTHLY;
        }
        if ("All".equalsIgnoreCase(label)) {
            return FILTER_ALL;
        }
        return Habit.FREQUENCY_DAILY;
    }

    private String labelForFrequency(String frequency) {
        if (Habit.FREQUENCY_WEEKLY.equals(frequency)) {
            return "Weekly";
        }
        if (Habit.FREQUENCY_MONTHLY.equals(frequency)) {
            return "Monthly";
        }
        return "Daily";
    }

    private String initials(String title) {
        String[] words = title == null ? new String[0] : title.trim().split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                builder.append(word.substring(0, 1).toUpperCase(Locale.US));
            }
            if (builder.length() == 2) {
                break;
            }
        }
        return builder.length() == 0 ? "H" : builder.toString();
    }

    private int colorForIndex(int index) {
        int[] colors = {
                Color.rgb(255, 132, 169),
                Color.rgb(255, 210, 79),
                Color.rgb(177, 106, 236),
                Color.rgb(71, 191, 179),
                Color.rgb(92, 178, 255),
                Color.rgb(72, 199, 142)
        };
        return colors[index % colors.length];
    }

    private GradientDrawable roundedDrawable(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private GradientDrawable circleDrawable(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color == 0 ? accentColor() : color);
        return drawable;
    }

    @Override
    public void onHabitStatsRequested() {
        showCompletionSummary();
    }

    @Override
    public void onHabitFilterRequested(View anchor) {
        showFilterMenu(anchor);
    }

    @Override
    public void onDestroyView() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).clearHabitToolbarController(this);
        }
        super.onDestroyView();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private int accentColor() {
        return color(R.color.colorAddAction);
    }

    private int surfaceColor() {
        return color(R.color.colorSurface);
    }

    private int onPrimaryColor() {
        return color(R.color.colorOnPrimary);
    }

    private int onSurfaceColor() {
        return color(R.color.colorOnSurface);
    }

    private int mutedColor() {
        return color(R.color.colorOnSurfaceVariant);
    }

    private int color(int colorRes) {
        return requireContext().getColor(colorRes);
    }
}
