package com.example.taskmanagerapp.ui.fragment;

import android.content.Intent;
import android.content.res.ColorStateList;
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
import com.example.taskmanagerapp.data.remote.RetrofitClient;
import com.example.taskmanagerapp.data.remote.dto.LogoutRequest;
import com.example.taskmanagerapp.ui.activity.GeneralSettingsActivity;
import com.example.taskmanagerapp.ui.activity.LoginActivity;
import com.example.taskmanagerapp.ui.activity.MainActivity;
import com.example.taskmanagerapp.utils.PreferenceHelper;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsFragment extends Fragment {
    private static final String[] THEME_LABELS = {"Light", "Dark", "System"};
    private static final String[] THEME_KEYS = {
            PreferenceHelper.THEME_LIGHT,
            PreferenceHelper.THEME_DARK,
            PreferenceHelper.THEME_SYSTEM
    };

    private FrameLayout root;
    private View mainPage;
    private PreferenceHelper preferenceHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        preferenceHelper = new PreferenceHelper(requireContext());
        root = new FrameLayout(requireContext());
        root.setBackgroundColor(color(R.color.colorBackground));

        mainPage = createMainPage();

        root.addView(mainPage);
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
        FrameLayout page = new FrameLayout(requireContext());

        LinearLayout content = pageContent();
        content.setPadding(dp(14), dp(14), dp(14), dp(118));
        content.addView(profileCard());
        content.addView(space(18));

        LinearLayout appearanceGroup = cardContainer();
        appearanceGroup.addView(settingsRow("AP", "Appearance", themeSummary(), v -> showThemeDialog(), true));
        appearanceGroup.addView(divider());
        appearanceGroup.addView(notificationRow());
        content.addView(appearanceGroup);
        content.addView(space(16));

        LinearLayout generalGroup = cardContainer();
        generalGroup.addView(settingsRow("DT", "Date & Time", null, v -> notImplemented("Date & Time"), true));
        generalGroup.addView(divider());
        generalGroup.addView(settingsRow("GN", "General", null, v -> openGeneralSettings(), true));
        content.addView(generalGroup);
        content.addView(space(16));

        LinearLayout aboutGroup = cardContainer();
        aboutGroup.addView(settingsRow("AB", "About", "v1.0", v -> Toast.makeText(requireContext(), "TaskManagerApp v1.0", Toast.LENGTH_SHORT).show(), true));
        content.addView(aboutGroup);

        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.setClipToPadding(false);
        scrollView.addView(content, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        page.addView(scrollView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        FrameLayout.LayoutParams signOutParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
        );
        signOutParams.setMargins(dp(18), 0, dp(18), dp(18));
        page.addView(signOutButton(), signOutParams);
        return page;
    }

    private View profileCard() {
        LinearLayout card = cardContainer();
        LinearLayout row = new LinearLayout(requireContext());
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(16), dp(14), dp(12), dp(14));
        row.setMinimumHeight(dp(88));
        row.setOnClickListener(v -> openAccountFragment());
        card.addView(row);

        row.addView(avatarView(dp(54), displayInitials(), 17));

        LinearLayout textColumn = new LinearLayout(requireContext());
        textColumn.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textParams.setMargins(dp(16), 0, dp(8), 0);
        row.addView(textColumn, textParams);

        TextView name = label(displayName(), 16, color(R.color.colorOnSurface), true);
        name.setSingleLine(true);
        textColumn.addView(name);

        TextView email = label(displayEmail(), 13, color(R.color.colorOnSurfaceVariant), false);
        email.setPadding(0, dp(6), 0, 0);
        email.setSingleLine(true);
        textColumn.addView(email);

        row.addView(arrow());
        return card;
    }

    private View notificationRow() {
        LinearLayout row = baseRow();
        row.setOnClickListener(null);
        row.addView(rowIcon("NT"));

        TextView titleView = label("Notifications", 16, color(R.color.colorOnSurface), false);
        row.addView(titleView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Switch toggle = new Switch(requireContext());
        toggle.setChecked(preferenceHelper.isNotificationsEnabled());
        tintSwitch(toggle);
        toggle.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            preferenceHelper.setNotificationsEnabled(isChecked);
            Toast.makeText(requireContext(), isChecked ? "Notifications enabled" : "Notifications disabled", Toast.LENGTH_SHORT).show();
        });
        row.setOnClickListener(v -> toggle.setChecked(!toggle.isChecked()));
        row.addView(toggle);
        return row;
    }

    private View settingsRow(String icon, String title, String value, View.OnClickListener listener, boolean showArrow) {
        LinearLayout row = baseRow();
        if (listener != null) {
            row.setOnClickListener(listener);
        }

        if (icon != null) {
            row.addView(rowIcon(icon));
        }

        TextView titleView = label(title, 16, color(R.color.colorOnSurface), false);
        row.addView(titleView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        if (value != null) {
            TextView valueView = label(value, 14, color(R.color.colorOnSurfaceVariant), false);
            valueView.setGravity(Gravity.END);
            valueView.setSingleLine(true);
            LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.7f);
            valueParams.setMargins(dp(8), 0, 0, 0);
            row.addView(valueView, valueParams);
        }

        if (showArrow) {
            row.addView(arrow());
        }
        return row;
    }

    private LinearLayout baseRow() {
        LinearLayout row = new LinearLayout(requireContext());
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(18), dp(12), dp(14), dp(12));
        row.setMinimumHeight(dp(58));
        row.setClickable(true);
        row.setFocusable(true);
        return row;
    }

    private TextView rowIcon(String text) {
        TextView iconView = label(text, 11, color(R.color.colorAddAction), true);
        iconView.setGravity(Gravity.CENTER);
        iconView.setBackground(roundedDrawable(color(R.color.colorPrimaryContainer), dp(10)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(34), dp(34));
        params.setMargins(0, 0, dp(14), 0);
        iconView.setLayoutParams(params);
        return iconView;
    }

    private LinearLayout cardContainer() {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_settings_card);
        card.setClipToOutline(true);
        return card;
    }

    private TextView signOutButton() {
        TextView button = label("Sign Out", 16, color(R.color.colorDangerButtonText), true);
        button.setGravity(Gravity.CENTER);
        button.setBackgroundResource(R.drawable.bg_settings_logout);
        button.setPadding(dp(18), dp(15), dp(18), dp(15));
        button.setMinHeight(dp(54));
        button.setOnClickListener(v -> logout());
        return button;
    }

    private TextView avatarView(int size, String text, int textSize) {
        TextView avatar = label(text, textSize, color(R.color.colorAddAction), true);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackgroundResource(R.drawable.bg_settings_avatar);
        avatar.setMinWidth(size);
        avatar.setMinHeight(size);
        return avatar;
    }

    private TextView arrow() {
        TextView arrow = label(">", 20, color(R.color.colorOnSurfaceVariant), false);
        arrow.setGravity(Gravity.CENTER);
        arrow.setPadding(dp(10), 0, 0, 0);
        return arrow;
    }

    private TextView label(String text, int sp, int textColor, boolean bold) {
        TextView view = new TextView(requireContext());
        view.setText(text);
        view.setTextSize(sp);
        view.setTextColor(textColor);
        view.setIncludeFontPadding(false);
        if (bold) {
            view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        return view;
    }

    private LinearLayout pageContent() {
        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(14), dp(16), dp(14), dp(18));
        return content;
    }

    private View divider() {
        View divider = new View(requireContext());
        divider.setBackgroundColor(color(R.color.colorDivider));
        divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        return divider;
    }

    private Space space(int dp) {
        Space space = new Space(requireContext());
        space.setLayoutParams(new LinearLayout.LayoutParams(1, dp(dp)));
        return space;
    }

    private GradientDrawable roundedDrawable(int fillColor, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(fillColor);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private void tintSwitch(Switch toggle) {
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{}
        };
        toggle.setThumbTintList(new ColorStateList(states, new int[]{
                color(R.color.colorOnPrimary),
                color(R.color.colorOnSurfaceVariant)
        }));
        toggle.setTrackTintList(new ColorStateList(states, new int[]{
                color(R.color.colorAddAction),
                color(R.color.colorSurfaceVariant)
        }));
    }

    private void openAccountFragment() {
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).showFullScreenFragment(new AccountFragment());
        }
    }

    private void openGeneralSettings() {
        startActivity(new Intent(requireContext(), GeneralSettingsActivity.class));
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

    private String themeSummary() {
        String currentTheme = preferenceHelper.getTheme();
        for (int i = 0; i < THEME_KEYS.length; i++) {
            if (THEME_KEYS[i].equals(currentTheme)) {
                return THEME_LABELS[i];
            }
        }
        return "System";
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
        if (name.isEmpty()) {
            return "U";
        }
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
        String refreshToken = preferenceHelper.getRefreshToken();
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            finishLogout();
            return;
        }

        RetrofitClient.getAuthApi().logout(new LogoutRequest(refreshToken)).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                finishLogout();
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                finishLogout();
            }
        });
    }

    private void finishLogout() {
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
    }
}
