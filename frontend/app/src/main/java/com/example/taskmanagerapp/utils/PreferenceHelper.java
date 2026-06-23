package com.example.taskmanagerapp.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class PreferenceHelper {
    private static final String TAG = "PreferenceHelper";
    public static final String PREF_NAME = "student_task_manager_prefs";

    public static final String KEY_THEME = "theme";
    public static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";
    public static final String KEY_SORT_BY = "sort_by";
    public static final String KEY_AUTH_TOKEN = "auth_token";
    public static final String KEY_AUTH_USER_ID = "auth_user_id";
    public static final String KEY_AUTH_USERNAME = "auth_username";
    public static final String KEY_AUTH_EMAIL = "auth_email";
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";
    public static final String THEME_SYSTEM = "system";

    private final SharedPreferences preferences;

    public PreferenceHelper(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Log.d(TAG, "PreferenceHelper initialized");
    }

    public void setTheme(String theme) {
        preferences.edit().putString(KEY_THEME, theme).apply();
        Log.d(TAG, "setTheme -> " + theme);
    }

    public String getTheme() {
        String value = preferences.getString(KEY_THEME, THEME_SYSTEM);
        Log.d(TAG, "getTheme -> " + value);
        return value;
    }

    public void setNotificationsEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply();
        Log.d(TAG, "setNotificationsEnabled -> " + enabled);
    }

    public boolean isNotificationsEnabled() {
        boolean value = preferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, true);
        Log.d(TAG, "isNotificationsEnabled -> " + value);
        return value;
    }

    public void setSortBy(String sortBy) {
        preferences.edit().putString(KEY_SORT_BY, sortBy).apply();
        Log.d(TAG, "setSortBy -> " + sortBy);
    }

    public String getSortBy() {
        String value = preferences.getString(KEY_SORT_BY, "deadline");
        Log.d(TAG, "getSortBy -> " + value);
        return value;
    }

    public void saveAuth(String token, String userId, String username, String email) {
        preferences.edit()
                .putString(KEY_AUTH_TOKEN, token)
                .putString(KEY_AUTH_USER_ID, userId)
                .putString(KEY_AUTH_USERNAME, username)
                .putString(KEY_AUTH_EMAIL, email)
                .apply();
        Log.d(TAG, "saveAuth -> " + username);
    }

    public String getAuthToken() {
        return preferences.getString(KEY_AUTH_TOKEN, null);
    }

    public String getAuthUserId() {
        return preferences.getString(KEY_AUTH_USER_ID, null);
    }

    public boolean hasAuthToken() {
        String token = getAuthToken();
        return token != null && !token.trim().isEmpty();
    }

    public void clearAuth() {
        preferences.edit()
                .remove(KEY_AUTH_TOKEN)
                .remove(KEY_AUTH_USER_ID)
                .remove(KEY_AUTH_USERNAME)
                .remove(KEY_AUTH_EMAIL)
                .apply();
        Log.d(TAG, "clearAuth");
    }

    public void resetToDefault() {
        preferences.edit()
                .putString(KEY_THEME, "system")
                .putBoolean(KEY_NOTIFICATIONS_ENABLED, true)
                .putString(KEY_SORT_BY, "deadline")
                .apply();
        Log.d(TAG, "resetToDefault -> theme=system, notifications=true, sort_by=deadline");
    }
}
