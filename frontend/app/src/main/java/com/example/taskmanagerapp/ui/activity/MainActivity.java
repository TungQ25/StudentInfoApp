package com.example.taskmanagerapp.ui.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.taskmanagerapp.R;
import com.example.taskmanagerapp.ui.fragment.SettingsFragment;
import com.example.taskmanagerapp.ui.fragment.TaskFragment;
import com.example.taskmanagerapp.utils.PreferenceHelper;
import com.google.android.material.appbar.MaterialToolbar;

public class MainActivity extends AppCompatActivity {
    private static final String STATE_SELECTED_BOTTOM_NAV_ITEM = "selectedBottomNavItem";

    private MaterialToolbar toolbar;
    private View bottomNavigation;
    private PreferenceHelper preferenceHelper;
    private String appliedTheme;
    private int selectedBottomNavItem = R.id.nav_task;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applySavedThemeMode();
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        initializeViews();

        if (!preferenceHelper.hasAuthToken()) {
            openLoginAndFinish();
            return;
        }

        if (savedInstanceState != null) {
            selectedBottomNavItem = savedInstanceState.getInt(STATE_SELECTED_BOTTOM_NAV_ITEM, R.id.nav_task);
        }

        // Handle window insets for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            // Điều chỉnh lề thanh điều hướng phía dưới để phù hợp với thanh điều hướng hệ thống
            if (bottomNavigation != null) {
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) bottomNavigation.getLayoutParams();
                params.bottomMargin = systemBars.bottom;
                bottomNavigation.setLayoutParams(params);
            }
            return insets;
        });

        setupBottomNavigation();
        if (getSupportFragmentManager().findFragmentById(R.id.main_fragment_container) == null) {
            showSelectedFragment(selectedBottomNavItem);
        } else {
            applyBottomNavigationState(selectedBottomNavItem);
        }
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        preferenceHelper = new PreferenceHelper(this);
        appliedTheme = preferenceHelper.getTheme();
    }

    private void setupBottomNavigation() {
        if (bottomNavigation == null) {
            return;
        }
        findViewById(R.id.nav_task).setOnClickListener(v -> showSelectedFragment(R.id.nav_task));
        findViewById(R.id.nav_settings).setOnClickListener(v -> showSelectedFragment(R.id.nav_settings));
        findViewById(R.id.nav_home).setOnClickListener(v -> showPendingNavigationItem());
        findViewById(R.id.nav_matrix).setOnClickListener(v -> showPendingNavigationItem());
        findViewById(R.id.nav_habit).setOnClickListener(v -> showPendingNavigationItem());
    }

    private void showSelectedFragment(int itemId) {
        selectedBottomNavItem = itemId;
        Fragment fragment;
        if (itemId == R.id.nav_settings) {
            fragment = new SettingsFragment();
        } else {
            fragment = new TaskFragment();
            itemId = R.id.nav_task;
            selectedBottomNavItem = R.id.nav_task;
        }

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_fragment_container, fragment)
                .commit();
        applyBottomNavigationState(itemId);
    }

    // Thêm đầy đủ chức năng thì bỏ đi
    private void showPendingNavigationItem() {
        selectBottomNavigationItem(selectedBottomNavItem);
        Toast.makeText(this, "Đang phát triển", Toast.LENGTH_SHORT).show();
    }

    private void applyBottomNavigationState(int selectedItemId) {
        selectBottomNavigationItem(selectedItemId);
        if (toolbar != null) {
            toolbar.setTitle(selectedItemId == R.id.nav_settings ? R.string.title_settings : R.string.title_main);
        }
    }

    private void selectBottomNavigationItem(int selectedItemId) {
        int[] itemIds = {
                R.id.nav_home,
                R.id.nav_matrix,
                R.id.nav_task,
                R.id.nav_habit,
                R.id.nav_settings
        };
        for (int itemId : itemIds) {
            View item = findViewById(itemId);
            if (item != null) {
                item.setSelected(itemId == selectedItemId);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(STATE_SELECTED_BOTTOM_NAV_ITEM, selectedBottomNavItem);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (preferenceHelper == null) {
            return;
        }
        if (!preferenceHelper.hasAuthToken()) {
            openLoginAndFinish();
            return;
        }
        String latestTheme = preferenceHelper.getTheme();
        if (!latestTheme.equals(appliedTheme)) {
            appliedTheme = latestTheme;
            applySavedThemeMode();
            recreate();
        }
    }

    private void applySavedThemeMode() {
        SharedPreferences preferences = getSharedPreferences(PreferenceHelper.PREF_NAME, MODE_PRIVATE);
        String theme = preferences.getString(PreferenceHelper.KEY_THEME, PreferenceHelper.THEME_SYSTEM);
        if (PreferenceHelper.THEME_LIGHT.equals(theme)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else if (PreferenceHelper.THEME_DARK.equals(theme)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    private void openLoginAndFinish() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}