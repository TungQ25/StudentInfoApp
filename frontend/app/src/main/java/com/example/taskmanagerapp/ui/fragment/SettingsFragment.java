package com.example.taskmanagerapp.ui.fragment;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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
    private static final String ARG_OVERLAY_MODE = "overlayMode";
    private static final long ENTER_DURATION_MS = 280L;
    private static final long EXIT_DURATION_MS = 220L;
    private static final long SCRIM_DURATION_MS = 180L;
    private static final String[] THEME_LABELS = {"Light", "Dark", "System"};
    private static final String[] THEME_KEYS = {
            PreferenceHelper.THEME_LIGHT,
            PreferenceHelper.THEME_DARK,
            PreferenceHelper.THEME_SYSTEM
    };

    private View rootView;
    private View scrimView;
    private View panelView;
    private View topBarView;
    private TextView avatarView;
    private TextView nameView;
    private TextView emailView;
    private TextView themeValueView;
    private Switch notificationsSwitch;
    private PreferenceHelper preferenceHelper;
    private boolean overlayMode;
    private boolean dismissing;

    public static SettingsFragment newOverlayInstance() {
        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_OVERLAY_MODE, true);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overlayMode = getArguments() != null && getArguments().getBoolean(ARG_OVERLAY_MODE, false);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rootView = view;
        preferenceHelper = new PreferenceHelper(requireContext());

        bindViews(view);
        configurePresentation();
        setupOverlayInsets();
        bindProfile();
        bindActions(view);

        if (overlayMode) {
            setupBackHandler();
            rootView.post(this::playEntranceAnimation);
        } else if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).showSimpleToolbar(getString(R.string.title_settings));
        }
    }

    private void bindViews(View view) {
        scrimView = view.findViewById(R.id.settingsScrim);
        panelView = view.findViewById(R.id.settingsPanel);
        topBarView = view.findViewById(R.id.settingsTopBar);
        avatarView = view.findViewById(R.id.tvSettingsAvatar);
        nameView = view.findViewById(R.id.tvSettingsName);
        emailView = view.findViewById(R.id.tvSettingsEmail);
        themeValueView = view.findViewById(R.id.tvSettingsThemeValue);
        notificationsSwitch = view.findViewById(R.id.switchSettingsNotifications);
    }

    private void configurePresentation() {
        if (topBarView != null) {
            topBarView.setVisibility(overlayMode ? View.VISIBLE : View.GONE);
        }
        if (scrimView != null) {
            scrimView.setVisibility(overlayMode ? View.VISIBLE : View.GONE);
            scrimView.setAlpha(0f);
            scrimView.setOnClickListener(overlayMode ? v -> dismissWithSlideDown() : null);
        }
        if (rootView != null) {
            rootView.setBackgroundColor(overlayMode ? Color.TRANSPARENT : color(R.color.colorBackground));
        }
        if (panelView != null) {
            panelView.setBackgroundResource(overlayMode ? R.drawable.bg_task_detail_dialog : 0);
            if (!overlayMode) {
                panelView.setBackgroundColor(color(R.color.colorBackground));
            }
            setPanelTopMargin(overlayMode ? dp(18) : 0);
            panelView.setElevation(overlayMode ? dp(56) : 0f);
            panelView.setTranslationY(overlayMode ? getResources().getDisplayMetrics().heightPixels : 0f);
        }
    }

    private void setupOverlayInsets() {
        if (!overlayMode || rootView == null) {
            return;
        }
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(rootView);
    }

    private void setPanelTopMargin(int topMargin) {
        if (panelView == null || !(panelView.getLayoutParams() instanceof ViewGroup.MarginLayoutParams)) {
            return;
        }
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) panelView.getLayoutParams();
        if (params.topMargin == topMargin) {
            return;
        }
        params.topMargin = topMargin;
        panelView.setLayoutParams(params);
    }

    private void bindProfile() {
        avatarView.setText(displayInitials());
        nameView.setText(displayName());
        emailView.setText(displayEmail());
        themeValueView.setText(themeSummary());
    }

    private void bindActions(View view) {
        view.findViewById(R.id.settingsProfileRow).setOnClickListener(v -> openAccountFragment());
        view.findViewById(R.id.settingsAppearanceRow).setOnClickListener(v -> showThemeDialog());
        view.findViewById(R.id.settingsDateTimeRow).setOnClickListener(v -> notImplemented("Date & Time"));
        view.findViewById(R.id.settingsGeneralRow).setOnClickListener(v -> openGeneralSettings());
        view.findViewById(R.id.settingsAboutRow).setOnClickListener(v ->
                Toast.makeText(requireContext(), "TaskManagerApp v1.0", Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.btnSettingsSignOut).setOnClickListener(v -> logout());

        View backButton = view.findViewById(R.id.btnSettingsBack);
        if (backButton != null) {
            backButton.setOnClickListener(v -> dismissWithSlideDown());
        }

        notificationsSwitch.setChecked(preferenceHelper.isNotificationsEnabled());
        tintSwitch(notificationsSwitch);
        notificationsSwitch.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            preferenceHelper.setNotificationsEnabled(isChecked);
            Toast.makeText(
                    requireContext(),
                    isChecked ? "Notifications enabled" : "Notifications disabled",
                    Toast.LENGTH_SHORT
            ).show();
        });
        view.findViewById(R.id.settingsNotificationRow).setOnClickListener(v ->
                notificationsSwitch.setChecked(!notificationsSwitch.isChecked()));
    }

    private void setupBackHandler() {
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                dismissWithSlideDown();
            }
        });
    }

    private void playEntranceAnimation() {
        if (rootView == null || panelView == null) {
            return;
        }

        panelView.setTranslationY(rootView.getHeight() + dp(32));
        panelView.animate()
                .translationY(0f)
                .setDuration(ENTER_DURATION_MS)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        if (scrimView != null) {
            scrimView.animate()
                    .alpha(1f)
                    .setDuration(SCRIM_DURATION_MS)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }
    }

    private void dismissWithSlideDown() {
        if (!overlayMode) {
            return;
        }
        if (dismissing) {
            return;
        }
        dismissing = true;
        if (panelView == null) {
            dismissSelf();
            return;
        }

        panelView.animate().cancel();
        if (scrimView != null) {
            scrimView.animate().cancel();
        }

        float target = rootView == null ? dp(720) : rootView.getHeight() + dp(32);
        if (scrimView != null) {
            scrimView.animate()
                    .alpha(0f)
                    .setDuration(SCRIM_DURATION_MS)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }
        panelView.animate()
                .translationY(target)
                .setDuration(EXIT_DURATION_MS)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(this::dismissSelf)
                .start();
    }

    private void dismissSelf() {
        getParentFragmentManager().popBackStack();
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
                    if (themeValueView != null) {
                        themeValueView.setText(THEME_LABELS[which]);
                    }
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
        if (panelView != null) {
            panelView.animate().cancel();
        }
        if (scrimView != null) {
            scrimView.animate().cancel();
        }
        super.onDestroyView();
        rootView = null;
        scrimView = null;
        panelView = null;
        topBarView = null;
        avatarView = null;
        nameView = null;
        emailView = null;
        themeValueView = null;
        notificationsSwitch = null;
    }
}
