package com.example.taskmanagerapp.ui.activity;

import android.content.Intent;
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

import com.example.taskmanagerapp.R;
import com.example.taskmanagerapp.utils.PreferenceHelper;

public class SettingsActivity extends AppCompatActivity {
    private Spinner spinnerTheme;
    private Switch switchNotifications;
    private Button btnReset;
    private Button btnLogout;
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
        btnLogout = findViewById(R.id.btnLogout);
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
            }
        });

        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) ->
                preferenceHelper.setNotificationsEnabled(isChecked)
        );

        btnReset.setOnClickListener(v -> {
            preferenceHelper.resetToDefault();
            loadSettings();
            Toast.makeText(this, "Settings reset", Toast.LENGTH_SHORT).show();
        });

        btnLogout.setOnClickListener(v -> {
            preferenceHelper.clearAuth();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
