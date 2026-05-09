package com.example.studentinfoapp;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SettingsActivity extends AppCompatActivity {
    private Spinner spinnerTheme;
    private Switch switchNotifications;
    private Button btnReset;
    private PreferenceHelper preferenceHelper;

    private static final String[] THEME_KEYS = {"light", "dark", "system"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        spinnerTheme = findViewById(R.id.spinnerTheme);
        switchNotifications = findViewById(R.id.switchNotifications);
        btnReset = findViewById(R.id.btnResetSettings);
        preferenceHelper = new PreferenceHelper(this);

        setupThemeSpinner();
        loadSettings();
        setupListeners();
    }

    private void setupThemeSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"Light", "Dark", "System"}
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTheme.setAdapter(adapter);
    }

    private void loadSettings() {
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
    }

    private void setupListeners() {
        spinnerTheme.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                preferenceHelper.setTheme(THEME_KEYS[position]);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                // No-op
            }
        });

        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) ->
                preferenceHelper.setNotificationsEnabled(isChecked)
        );

        btnReset.setOnClickListener(v -> {
            preferenceHelper.resetToDefault();
            loadSettings();
            Toast.makeText(this, "Đã reset cài đặt về mặc định", Toast.LENGTH_SHORT).show();
        });
    }
}
