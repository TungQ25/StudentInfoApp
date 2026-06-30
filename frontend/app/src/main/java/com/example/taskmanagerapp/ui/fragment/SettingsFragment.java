package com.example.taskmanagerapp.ui.fragment;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.example.taskmanagerapp.R;
import com.example.taskmanagerapp.ui.activity.MainActivity;
import com.example.taskmanagerapp.ui.activity.LoginActivity;
import com.example.taskmanagerapp.utils.PreferenceHelper;

public class SettingsFragment extends Fragment {
    private static final String[] THEME_LABELS = {"Light", "Dark", "System"};
    private static final String[] THEME_KEYS = {
            PreferenceHelper.THEME_LIGHT,
            PreferenceHelper.THEME_DARK,
            PreferenceHelper.THEME_SYSTEM
    };

    private FrameLayout root;
    private View mainPage;
    private View generalPage;
    private View accountPage;
    private PreferenceHelper preferenceHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        preferenceHelper = new PreferenceHelper(requireContext());
        root = new FrameLayout(requireContext());
        root.setBackgroundColor(color(R.color.colorBackground));
        mainPage = createMainPage();
        generalPage = createGeneralPage();
        accountPage = createAccountPage();
        root.addView(mainPage);
        root.addView(generalPage);
        root.addView(accountPage);
        showPage(mainPage);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).showSimpleToolbar(getString(R.string.title_settings));
        }
    }

    private View createMainPage() {
        LinearLayout content = pageContent();
        content.addView(accountHeader());
        content.addView(space(22));
        content.addView(singleCardRow("TB", "Tab Bar", v -> notImplemented("Tab Bar")));
        content.addView(space(16));

        // TODO: cập nhật icon UI
        LinearLayout group = cardContainer();
        group.addView(settingsRow("AP", "Appearance", null, v -> showThemeDialog(), false));
        group.addView(settingsRow("SN", "Sounds & Notifications", notificationSummary(), v -> toggleNotifications(), false));
        group.addView(settingsRow("DT", "Date & Time", null, v -> notImplemented("Date & Time"), false));
        group.addView(settingsRow("GN", "General", null, v -> showPage(generalPage), false));
        content.addView(group);
        return scrollPage(content);
    }

    private View createGeneralPage() {
        LinearLayout content = pageContent();
        content.addView(header("General", v -> showPage(mainPage), null));

        LinearLayout topGroup = cardContainer();
        topGroup.addView(settingsRow(null, "Language", "English", v -> notImplemented("Language"), false));
        topGroup.addView(settingsRow(null, "Swipe Actions", null, v -> notImplemented("Swipe Actions"), false));
        topGroup.addView(taskContextMenuRow());
        content.addView(topGroup);
        content.addView(space(16));

        content.addView(singleCardRow(null, "Task Detail Page", v -> notImplemented("Task Detail Page")));
        content.addView(space(16));

        LinearLayout taskGroup = cardContainer();
        taskGroup.addView(settingsRow(null, "Smart Recognition", null, v -> notImplemented("Smart Recognition"), false));
        taskGroup.addView(settingsRow(null, "Task Quick Add", null, v -> notImplemented("Task Quick Add"), false));
        taskGroup.addView(settingsRow(null, "Task Default", null, v -> notImplemented("Task Default"), false));
        taskGroup.addView(settingsRow(null, "Upload/Download Attachments", null, v -> notImplemented("Attachments"), false));
        taskGroup.addView(settingsRow(null, "Collaborate", null, v -> notImplemented("Collaborate"), false));
        taskGroup.addView(settingsRow(null, "Manage Template", null, v -> notImplemented("Manage Template"), false));
        content.addView(taskGroup);
        content.addView(space(16));

        content.addView(singleCardRow(null, "Passcode & Face ID", v -> notImplemented("Passcode & Face ID")));
        content.addView(space(28));
        content.addView(logoutButton());
        content.addView(space(24));
        return scrollPage(content);
    }

    private View createAccountPage() {
        LinearLayout content = pageContent();
        content.addView(header("Account", v -> showPage(mainPage), "OK"));

        LinearLayout accountGroup = cardContainer();
        accountGroup.addView(accountRow("Avatar", "TM", v -> notImplemented("Avatar")));
        accountGroup.addView(settingsRow(null, "Nickname", displayName(), v -> notImplemented("Nickname"), true));
        accountGroup.addView(settingsRow(null, "Email", displayEmail(), v -> notImplemented("Email"), true));
        accountGroup.addView(settingsRow(null, "Create Passkey", null, v -> notImplemented("Passkey"), false));
        accountGroup.addView(settingsRow(null, "2-Step Verification", null, v -> notImplemented("2-Step Verification"), false));
        accountGroup.addView(settingsRow(null, "Device Management", null, v -> notImplemented("Device Management"), false));
        content.addView(accountGroup);

        Space fill = new Space(requireContext());
        content.addView(fill, new LinearLayout.LayoutParams(1, dp(220)));
        content.addView(deleteAccountButton());
        content.addView(space(24));
        return scrollPage(content);
    }

    private View accountHeader() {
        LinearLayout row = new LinearLayout(requireContext());
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(8), dp(22), dp(8), dp(22));
        row.setOnClickListener(v -> showPage(accountPage));

        TextView avatar = avatarView(dp(96), displayInitials(), 28);
        row.addView(avatar);

        LinearLayout textColumn = new LinearLayout(requireContext());
        textColumn.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textParams.setMargins(dp(18), 0, dp(8), 0);
        row.addView(textColumn, textParams);

        TextView name = label(displayName(), 28, Color.WHITE, true);
        textColumn.addView(name);

        row.addView(arrow());
        return row;
    }

    private View taskContextMenuRow() {
        LinearLayout row = new LinearLayout(requireContext());
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(22), dp(14), dp(18), dp(14));

        LinearLayout texts = new LinearLayout(requireContext());
        texts.setOrientation(LinearLayout.VERTICAL);
        row.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        texts.addView(label("Task Context Menu", 21, Color.WHITE, false));
        TextView summary = label("Long press task and habit to show context menu.", 14, Color.rgb(132, 132, 132), false);
        summary.setPadding(0, dp(6), dp(12), 0);
        texts.addView(summary);

        Switch toggle = new Switch(requireContext());
        toggle.setChecked(true);
        toggle.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) ->
                Toast.makeText(requireContext(), isChecked ? "Context menu enabled" : "Context menu disabled", Toast.LENGTH_SHORT).show());
        row.addView(toggle);
        return row;
    }

    private View settingsRow(String icon, String title, String value, View.OnClickListener listener, boolean dimValue) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(22), dp(18), dp(18), dp(18));
        row.setMinimumHeight(dp(74));
        row.setOnClickListener(listener);

        if (icon != null) {
            TextView iconView = label(icon, 18, Color.rgb(255, 122, 26), true);
            iconView.setGravity(Gravity.CENTER);
            row.addView(iconView, new LinearLayout.LayoutParams(dp(50), dp(40)));
        }

        TextView titleView = label(title, 23, Color.WHITE, false);
        row.addView(titleView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (value != null) {
            TextView valueView = label(value, 20, Color.rgb(132, 132, 132), false);
            valueView.setGravity(Gravity.END);
            valueView.setSingleLine(true);
            valueView.setAlpha(dimValue ? 0.75f : 1f);
            row.addView(valueView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        row.addView(arrow());
        return row;
    }

    private View accountRow(String title, String initials, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(22), dp(14), dp(18), dp(14));
        row.setMinimumHeight(dp(76));
        row.setOnClickListener(listener);
        row.addView(label(title, 23, Color.WHITE, false), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(avatarView(dp(52), initials, 14));
        row.addView(arrow());
        return row;
    }

    private View singleCardRow(String icon, String title, View.OnClickListener listener) {
        LinearLayout card = cardContainer();
        card.addView(settingsRow(icon, title, null, listener, false));
        return card;
    }

    private LinearLayout cardContainer() {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_settings_card);
        card.setClipToOutline(true);
        return card;
    }

    private View header(String title, View.OnClickListener backListener, String actionText) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(8), 0, dp(24));

        TextView back = circularButton("<", 28, Color.WHITE, Color.rgb(31, 31, 31));
        back.setOnClickListener(backListener);
        row.addView(back);

        TextView titleView = label(title, 22, Color.WHITE, true);
        titleView.setGravity(Gravity.CENTER);
        row.addView(titleView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView action = circularButton(actionText == null ? "" : actionText, 15, Color.WHITE, actionText == null ? Color.TRANSPARENT : Color.rgb(255, 122, 26));
        row.addView(action);
        return row;
    }

    private TextView logoutButton() {
        TextView button = label("Log out", 20, Color.rgb(255, 122, 26), true);
        button.setGravity(Gravity.CENTER);
        button.setBackgroundResource(R.drawable.bg_settings_card);
        button.setPadding(dp(18), dp(18), dp(18), dp(18));
        button.setOnClickListener(v -> logout());
        return button;
    }

    private TextView deleteAccountButton() {
        TextView button = label("Delete Account", 20, Color.rgb(255, 76, 84), true);
        button.setGravity(Gravity.CENTER);
        button.setBackgroundResource(R.drawable.bg_settings_logout);
        button.setPadding(dp(18), dp(18), dp(18), dp(18));
        button.setOnClickListener(v -> notImplemented("Delete Account"));
        return button;
    }

    private TextView avatarView(int size, String text, int textSize) {
        TextView avatar = label(text, textSize, Color.rgb(70, 70, 70), true);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackgroundResource(R.drawable.bg_settings_avatar);
        avatar.setMinWidth(size);
        avatar.setMinHeight(size);
        return avatar;
    }

    private TextView arrow() {
        TextView arrow = label(">", 28, Color.rgb(112, 112, 112), false);
        arrow.setGravity(Gravity.CENTER);
        arrow.setPadding(dp(10), 0, 0, 0);
        return arrow;
    }

    private TextView circularButton(String text, int textSize, int textColor, int fillColor) {
        TextView button = label(text, textSize, textColor, true);
        button.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(fillColor);
        if (fillColor != Color.TRANSPARENT) {
            bg.setStroke(dp(1), Color.rgb(55, 55, 55));
        }
        button.setBackground(bg);
        button.setMinWidth(dp(64));
        button.setMinHeight(dp(64));
        return button;
    }

    private TextView label(String text, int sp, int color, boolean bold) {
        TextView view = new TextView(requireContext());
        view.setText(text);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setIncludeFontPadding(true);
        if (bold) {
            view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        return view;
    }

    private LinearLayout pageContent() {
        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(28), dp(18), dp(18));
        return content;
    }

    private ScrollView scrollPage(LinearLayout content) {
        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.setClipToPadding(false);
        scrollView.addView(content, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return scrollView;
    }

    private Space space(int dp) {
        Space space = new Space(requireContext());
        space.setLayoutParams(new LinearLayout.LayoutParams(1, dp(dp)));
        return space;
    }

    private void showPage(View page) {
        mainPage.setVisibility(page == mainPage ? View.VISIBLE : View.GONE);
        generalPage.setVisibility(page == generalPage ? View.VISIBLE : View.GONE);
        accountPage.setVisibility(page == accountPage ? View.VISIBLE : View.GONE);
    }

    private void showThemeDialog() {
        String currentTheme = preferenceHelper.getTheme();
        int checked = 2;
        for (int i = 0; i < THEME_KEYS.length; i++) {
            if (THEME_KEYS[i].equals(currentTheme)) {
                checked = i;
                break;
            }
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("Appearance")
                .setSingleChoiceItems(THEME_LABELS, checked, (dialog, which) -> {
                    String selectedTheme = THEME_KEYS[which];
                    preferenceHelper.setTheme(selectedTheme);
                    applyThemeMode(selectedTheme);
                    dialog.dismiss();
                    requireActivity().recreate();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void toggleNotifications() {
        boolean enabled = !preferenceHelper.isNotificationsEnabled();
        preferenceHelper.setNotificationsEnabled(enabled);
        Toast.makeText(requireContext(), enabled ? "Notifications enabled" : "Notifications disabled", Toast.LENGTH_SHORT).show();
    }

    private String notificationSummary() {
        return preferenceHelper.isNotificationsEnabled() ? "On" : "Off";
    }

    private String displayName() {
        String username = preferenceHelper.getAuthUsername();
        return username == null || username.trim().isEmpty() ? "User" : username;
    }

    private String displayEmail() {
        String email = preferenceHelper.getAuthEmail();
        return email == null || email.trim().isEmpty() ? "No email" : email;
    }

    private String displayInitials() {
        String name = displayName().trim();
        if (name.isEmpty()) return "U";
        String[] parts = name.split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase();
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }

    private void notImplemented(String feature) {
        Toast.makeText(requireContext(), feature + " is not implemented yet", Toast.LENGTH_SHORT).show();
    }

    private void logout() {
        preferenceHelper.clearAuth();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    private void applyThemeMode(String theme) {
        if (PreferenceHelper.THEME_LIGHT.equals(theme)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (PreferenceHelper.THEME_DARK.equals(theme)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private int color(int colorRes) {
        return requireContext().getColor(colorRes);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        root = null;
        mainPage = null;
        generalPage = null;
        accountPage = null;
    }
}
