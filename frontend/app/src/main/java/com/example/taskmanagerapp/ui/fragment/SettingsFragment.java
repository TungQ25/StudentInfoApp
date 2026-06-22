package com.example.taskmanagerapp.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.example.taskmanagerapp.R;
import com.example.taskmanagerapp.ui.activity.LoginActivity;
import com.example.taskmanagerapp.utils.PreferenceHelper;

public class SettingsFragment extends Fragment {
    private static final String[] THEME_KEYS = {"light", "dark", "system"};

    private Spinner spinnerTheme;
    private Switch switchNotifications;
    private Button btnReset;
    private Button btnLogout;
    private PreferenceHelper preferenceHelper;
    private boolean loadingSettings;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        spinnerTheme = view.findViewById(R.id.spinnerTheme);
        switchNotifications = view.findViewById(R.id.switchNotifications);
        btnReset = view.findViewById(R.id.btnResetSettings);
        btnLogout = view.findViewById(R.id.btnLogout);
        preferenceHelper = new PreferenceHelper(requireContext());

        setupThemeSpinner();
        loadSettings();
        setupListeners();
    }

    private void setupThemeSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"Light", "Dark", "System"}
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTheme.setAdapter(adapter);
    }

    private void loadSettings() {
        loadingSettings = true;
        String currentTheme = preferenceHelper.getTheme();
        int themePosition = 2;
        for (int i = 0; i < THEME_KEYS.length; i++) {
            if (THEME_KEYS[i].equals(currentTheme)) {
                themePosition = i;
                break;
            }
        }
        spinnerTheme.setSelection(themePosition);
        switchNotifications.setChecked(preferenceHelper.isNotificationsEnabled());
        loadingSettings = false;
    }

    private void setupListeners() {
        spinnerTheme.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (loadingSettings) {
                    return;
                }
                String selectedTheme = THEME_KEYS[position];
                if (!selectedTheme.equals(preferenceHelper.getTheme())) {
                    preferenceHelper.setTheme(selectedTheme);
                    applyThemeMode(selectedTheme);
                    requireActivity().recreate();
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) ->
                preferenceHelper.setNotificationsEnabled(isChecked)
        );

        btnReset.setOnClickListener(v -> {
            preferenceHelper.resetToDefault();
            loadSettings();
            Toast.makeText(requireContext(), "Settings reset", Toast.LENGTH_SHORT).show();
        });

        btnLogout.setOnClickListener(v -> {
            preferenceHelper.clearAuth();
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        });
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
}
