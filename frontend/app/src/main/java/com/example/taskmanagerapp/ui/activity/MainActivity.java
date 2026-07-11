package com.example.taskmanagerapp.ui.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.taskmanagerapp.R;
import com.example.taskmanagerapp.sync.SyncManager;
import com.example.taskmanagerapp.ui.fragment.HabitFragment;
import com.example.taskmanagerapp.ui.fragment.MatrixFragment;
import com.example.taskmanagerapp.ui.fragment.PomodoroFragment;
import com.example.taskmanagerapp.ui.fragment.SettingsFragment;
import com.example.taskmanagerapp.ui.fragment.TaskFragment;
import com.example.taskmanagerapp.utils.PreferenceHelper;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    // Giao sự kiện cho fragment xử lý
    public interface TaskToolbarController {
        void onToggleSidebarRequested();
        void onToolbarMoreRequested(View anchor);
    }

    public interface HabitToolbarController {
        void onHabitStatsRequested();
        void onHabitFilterRequested(View anchor);
    }

    private static final String TAG = "MainActivity";
    private static final String STATE_SELECTED_BOTTOM_NAV_ITEM = "selectedBottomNavItem";

    private View appBarLayout;
    private MaterialToolbar toolbar;
    private TextView toolbarTitle;
    private TextView toolbarTitleIcon;
    private ImageButton toolbarLeftButton;
    private ImageButton toolbarRightButton;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View bottomNavigation;
    private View sidebarPanel;
    private View sidebarScrim;
    private View fullScreenFragmentContainer;
    private TaskToolbarController taskToolbarController;
    private HabitToolbarController habitToolbarController;
    private PreferenceHelper preferenceHelper;
    private SyncManager syncManager;
    private final ExecutorService refreshExecutor = Executors.newSingleThreadExecutor();
    private String appliedTheme;
    private int selectedBottomNavItem = R.id.nav_task;
    private boolean isManualRefreshRunning;

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
            setBottomMargin(bottomNavigation, systemBars.bottom);
            setBottomMargin(sidebarPanel, systemBars.bottom);
            setBottomMargin(sidebarScrim, systemBars.bottom);
            return insets;
        });

        setupPullToRefresh();
        setupBottomNavigation();
        if (getSupportFragmentManager().findFragmentById(R.id.main_fragment_container) == null) {
            showSelectedFragment(selectedBottomNavItem);
        } else {
            applyBottomNavigationState(selectedBottomNavItem);
        }
        updatePullToRefreshEnabled();
        getSupportFragmentManager().addOnBackStackChangedListener(this::updateFullScreenFragmentContainer);
        updateFullScreenFragmentContainer();
    }

    private void initializeViews() {
        appBarLayout = findViewById(R.id.appBarLayout);
        toolbar = findViewById(R.id.toolbar);
        toolbarTitle = findViewById(R.id.tvSelectedFilter);
        toolbarTitleIcon = findViewById(R.id.ivToolbarTitleIcon);
        toolbarLeftButton = findViewById(R.id.btnToggleSidebar);
        toolbarRightButton = findViewById(R.id.btnToolbarMore);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        if (toolbarLeftButton != null) {
            toolbarLeftButton.setOnClickListener(v -> {
                if (taskToolbarController != null) {
                    taskToolbarController.onToggleSidebarRequested(); // Mở sidebar
                } else if (habitToolbarController != null) {
                    habitToolbarController.onHabitStatsRequested();
                }
            });
        }
        if (toolbarRightButton != null) {
            toolbarRightButton.setOnClickListener(v -> {
                if (taskToolbarController != null) {
                    taskToolbarController.onToolbarMoreRequested(v); // Mở menu tùy chọn
                } else if (habitToolbarController != null) {
                    habitToolbarController.onHabitFilterRequested(v);
                }
            });
        }
        bottomNavigation = findViewById(R.id.bottomNavigation);
        sidebarPanel = findViewById(R.id.sidebarPanel);
        sidebarScrim = findViewById(R.id.sidebarScrim);
        fullScreenFragmentContainer = findViewById(R.id.fullScreenFragmentContainer);
        preferenceHelper = new PreferenceHelper(this);
        syncManager = new SyncManager(this);
        appliedTheme = preferenceHelper.getTheme();
    }

    private void setupPullToRefresh() {
        if (swipeRefreshLayout == null) {
            return;
        }
        swipeRefreshLayout.setColorSchemeResources(R.color.colorAddAction);
        swipeRefreshLayout.setProgressBackgroundColorSchemeResource(R.color.colorSurface);
        swipeRefreshLayout.setOnChildScrollUpCallback((parent, child) ->
                !isPullToRefreshAllowed() || isCurrentFragmentScrolledUp());
        swipeRefreshLayout.setOnRefreshListener(this::performManualRefresh);
    }

    private void performManualRefresh() {
        if (!isPullToRefreshAllowed()) {
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(false);
            }
            return;
        }
        if (isManualRefreshRunning) {
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(true);
            }
            return;
        }

        isManualRefreshRunning = true;
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(true);
        }

        refreshExecutor.execute(() -> {
            boolean synced = false;
            try {
                synced = syncManager.syncNow();
            } catch (Exception e) {
                Log.e(TAG, "Manual refresh sync failed", e);
            }
            boolean finalSynced = synced;
            runOnUiThread(() -> finishManualRefresh(finalSynced));
        });
    }

    private void finishManualRefresh(boolean synced) {
        if (isDestroyed()) {
            return;
        }
        isManualRefreshRunning = false;
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
        updatePullToRefreshEnabled();

        if (preferenceHelper != null && !preferenceHelper.hasAuthToken()) {
            openLoginAndFinish();
            return;
        }
    }

    private void updatePullToRefreshEnabled() {
        if (swipeRefreshLayout == null) {
            return;
        }
        swipeRefreshLayout.setEnabled(isPullToRefreshAllowed());
        if (!swipeRefreshLayout.isEnabled() && !isManualRefreshRunning) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private boolean isPullToRefreshAllowed() {
        boolean fullScreenVisible = fullScreenFragmentContainer != null
                && fullScreenFragmentContainer.getVisibility() == View.VISIBLE;
        boolean sidebarVisible = sidebarPanel != null && sidebarPanel.getVisibility() == View.VISIBLE;
        return selectedBottomNavItem != R.id.nav_matrix && !fullScreenVisible && !sidebarVisible;
    }

    private boolean isCurrentFragmentScrolledUp() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.main_fragment_container);
        View fragmentView = fragment == null ? null : fragment.getView();
        return canScrollUp(fragmentView);
    }

    private boolean canScrollUp(View view) {
        if (view == null || view.getVisibility() != View.VISIBLE) {
            return false;
        }
        if (view.canScrollVertically(-1)) {
            return true;
        }
        if (!(view instanceof ViewGroup)) {
            return false;
        }
        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            if (canScrollUp(group.getChildAt(i))) {
                return true;
            }
        }
        return false;
    }

    private void setBottomMargin(View view, int bottomMargin) {
        if (view == null || !(view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams)) {
            return;
        }
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        if (params.bottomMargin == bottomMargin) {
            return;
        }
        params.bottomMargin = bottomMargin;
        view.setLayoutParams(params);
    }

    private void setupBottomNavigation() {
        if (bottomNavigation == null) {
            return;
        }
        findViewById(R.id.nav_task).setOnClickListener(v -> showSelectedFragment(R.id.nav_task));
        findViewById(R.id.nav_matrix).setOnClickListener(v -> showSelectedFragment(R.id.nav_matrix));
        findViewById(R.id.nav_settings).setOnClickListener(v -> showSelectedFragment(R.id.nav_settings));
        findViewById(R.id.nav_pomodoro).setOnClickListener(v -> showSelectedFragment(R.id.nav_pomodoro));
        findViewById(R.id.nav_habit).setOnClickListener(v -> showSelectedFragment(R.id.nav_habit));
    }

    private void showSelectedFragment(int itemId) {
        selectedBottomNavItem = itemId;
        Fragment fragment;
        if (itemId == R.id.nav_settings) {
            fragment = new SettingsFragment();
        } else if (itemId == R.id.nav_pomodoro) {
            fragment = new PomodoroFragment();
        } else if (itemId == R.id.nav_matrix) {
            fragment = new MatrixFragment();
        } else if (itemId == R.id.nav_habit) {
            fragment = new HabitFragment();
        } else {
            fragment = new TaskFragment();
            itemId = R.id.nav_task;
            selectedBottomNavItem = R.id.nav_task;
        }

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_fragment_container, fragment)
                .commit();
        applyBottomNavigationState(itemId);
        updatePullToRefreshEnabled();
    }

    /**
     * Cập nhật trạng thái thanh điều hướng bên dưới.
     * @param selectedItemId
     */
    private void applyBottomNavigationState(int selectedItemId) {
        selectBottomNavigationItem(selectedItemId);
    }

    /**
     * Hiển thị toolbar đơn giản chỉ có tiêu đề, ẩn các nút chức năng.
     * @param title Tiêu đề hiển thị trên toolbar
     */
    public void showSimpleToolbar(String title) {
        taskToolbarController = null;
        habitToolbarController = null;
        configureToolbar(
                title,
                R.drawable.ic_hamburger_large,
                R.string.toggle_sidebar,
                View.INVISIBLE,
                R.drawable.ic_more_horizontal,
                R.string.toolbar_more_options,
                View.INVISIBLE
        );
    }

    public void showHabitToolbar(HabitToolbarController controller) {
        taskToolbarController = null;
        habitToolbarController = controller;
        configureToolbar(
                getString(R.string.title_habit),
                R.drawable.ic_habit_stats,
                R.string.habit_stats,
                View.VISIBLE,
                R.drawable.ic_habit_sliders,
                R.string.habit_filter,
                View.VISIBLE
        );
    }

    public void showTaskToolbar(String title, TaskToolbarController controller) {
        taskToolbarController = controller;
        habitToolbarController = null;
        configureToolbar(
                title,
                R.drawable.ic_hamburger_large,
                R.string.toggle_sidebar,
                View.VISIBLE,
                R.drawable.ic_more_horizontal,
                R.string.toolbar_more_options,
                View.VISIBLE
        );
        setToolbarTitleIconVisible(true);
    }

    public void showMatrixToolbar(String title, TaskToolbarController controller) {
        taskToolbarController = controller;
        habitToolbarController = null;
        configureToolbar(
                title,
                R.drawable.ic_hamburger_large,
                R.string.toggle_sidebar,
                View.INVISIBLE,
                R.drawable.ic_more_horizontal,
                R.string.toolbar_more_options,
                View.VISIBLE
        );
    }

    public void setTaskToolbarTitle(String title) {
        if (toolbarTitle != null) {
            toolbarTitle.setText(title == null || title.trim().isEmpty() ? "All" : title);
        }
    }

    public void setTaskToolbarTitleIcon(String icon) {
        if (toolbarTitleIcon != null) {
            String cleanIcon = icon == null ? "" : icon.trim();
            toolbarTitleIcon.setText(cleanIcon.isEmpty() || "#".equals(cleanIcon) ? "\u25CE" : cleanIcon);
        }
    }

    public void showFullScreenFragment(Fragment fragment) {
        if (fragment == null || fullScreenFragmentContainer == null) {
            return;
        }
        fullScreenFragmentContainer.setVisibility(View.VISIBLE);
        fullScreenFragmentContainer.bringToFront();
        updatePullToRefreshEnabled();
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fullScreenFragmentContainer, fragment)
                .addToBackStack("fullScreenFragment")
                .commit();
    }

    public void clearTaskToolbarController(TaskToolbarController controller) {
        if (taskToolbarController == controller) {
            taskToolbarController = null;
        }
    }

    public void clearHabitToolbarController(HabitToolbarController controller) {
        if (habitToolbarController == controller) {
            habitToolbarController = null;
        }
    }

    private void configureToolbar(String title,
                                  int leftIconRes,
                                  int leftDescriptionRes,
                                  int leftVisibility,
                                  int rightIconRes,
                                  int rightDescriptionRes,
                                  int rightVisibility) {
        showMainToolbar();
        if (toolbar != null) {
            toolbar.setTitle("");
        }
        setToolbarTitleIconVisible(false);
        setToolbarButton(toolbarLeftButton, leftIconRes, leftDescriptionRes, leftVisibility);
        setToolbarButton(toolbarRightButton, rightIconRes, rightDescriptionRes, rightVisibility);
        setTaskToolbarTitle(title);
    }

    private void setToolbarButton(ImageButton button, int iconRes, int descriptionRes, int visibility) {
        if (button == null) {
            return;
        }
        button.setImageResource(iconRes);
        button.setColorFilter(Color.rgb(215, 221, 229));
        button.setContentDescription(getString(descriptionRes));
        button.setVisibility(visibility);
    }

    private void setToolbarTitleIconVisible(boolean visible) {
        if (toolbarTitleIcon != null) {
            toolbarTitleIcon.setVisibility(visible ? View.VISIBLE : View.GONE);
            toolbarTitleIcon.setTextColor(Color.rgb(215, 221, 229));
        }
        if (toolbarTitle != null) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) toolbarTitle.getLayoutParams();
            params.leftMargin = visible ? dp(8) : 0;
            toolbarTitle.setLayoutParams(params);
        }
    }

    private void showMainToolbar() {
        if (appBarLayout != null) {
            appBarLayout.setVisibility(View.VISIBLE);
        }
    }

    private void updateFullScreenFragmentContainer() {
        if (fullScreenFragmentContainer == null) {
            return;
        }
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fullScreenFragmentContainer);
        fullScreenFragmentContainer.setVisibility(fragment == null ? View.GONE : View.VISIBLE);
        if (fragment != null) {
            fullScreenFragmentContainer.bringToFront();
        }
        updatePullToRefreshEnabled();
    }

    private void selectBottomNavigationItem(int selectedItemId) {
        int[] itemIds = {
                R.id.nav_pomodoro,
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
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
        updatePullToRefreshEnabled();
    }

    @Override
    protected void onDestroy() {
        refreshExecutor.shutdownNow();
        super.onDestroy();
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
