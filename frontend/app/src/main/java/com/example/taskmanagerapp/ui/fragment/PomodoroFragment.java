package com.example.taskmanagerapp.ui.fragment;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.taskmanagerapp.R;
import com.example.taskmanagerapp.ui.activity.MainActivity;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.Locale;

public class PomodoroFragment extends Fragment {
    private static final long DEFAULT_FOCUS_MS = 25 * 60 * 1000L;
    private static final long DEFAULT_SHORT_BREAK_MS = 5 * 60 * 1000L;
    private static final long DEFAULT_LONG_BREAK_MS = 15 * 60 * 1000L;
    private static final int DAILY_GOAL = 5;

    private TextView tvFocusTaskTitle;
    private TextView tvSubtitle;
    private TextView tvTime;
    private TextView tvRounds;
    private TextView tvFocusTime;
    private TextView tvGoal;
    private TextView btnStartPause;
    private TextView btnReset;
    private TextView tabFocus;
    private TextView tabShortBreak;
    private TextView tabLongBreak;
    private CircularProgressIndicator progress;
    private View focusTaskPicker;
    private View timerTapTarget;
    private CountDownTimer timer;
    private long focusDurationMs = DEFAULT_FOCUS_MS;
    private long shortBreakDurationMs = DEFAULT_SHORT_BREAK_MS;
    private long longBreakDurationMs = DEFAULT_LONG_BREAK_MS;
    private long durationMs = DEFAULT_FOCUS_MS;
    private long remainingMs = DEFAULT_FOCUS_MS;
    private boolean running = false;
    private int focusSessions = 0;
    private Mode mode = Mode.FOCUS;

    private enum Mode {
        FOCUS,
        SHORT_BREAK,
        LONG_BREAK
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pomodoro, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvFocusTaskTitle = view.findViewById(R.id.tvFocusTaskTitle);
        tvSubtitle = view.findViewById(R.id.tvPomodoroSubtitle);
        tvTime = view.findViewById(R.id.tvPomodoroTime);
        tvRounds = view.findViewById(R.id.tvPomodoroRounds);
        tvFocusTime = view.findViewById(R.id.tvPomodoroFocusTime);
        tvGoal = view.findViewById(R.id.tvPomodoroGoal);
        btnStartPause = view.findViewById(R.id.btnPomodoroStartPause);
        btnReset = view.findViewById(R.id.btnPomodoroReset);
        progress = view.findViewById(R.id.progressPomodoro);
        tabFocus = view.findViewById(R.id.btnPomodoroFocus);
        tabShortBreak = view.findViewById(R.id.btnPomodoroShortBreak);
        tabLongBreak = view.findViewById(R.id.btnPomodoroLongBreak);
        focusTaskPicker = view.findViewById(R.id.focusTaskPicker);
        timerTapTarget = view.findViewById(R.id.timerTapTarget);

        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).showSimpleToolbar(getString(R.string.title_pomodoro));
        }

        tabFocus.setOnClickListener(v -> switchMode(Mode.FOCUS));
        tabShortBreak.setOnClickListener(v -> switchMode(Mode.SHORT_BREAK));
        tabLongBreak.setOnClickListener(v -> switchMode(Mode.LONG_BREAK));
        focusTaskPicker.setOnClickListener(v -> showTaskPickerPlaceholder());
        timerTapTarget.setOnClickListener(v -> showDurationDialog());
        btnStartPause.setOnClickListener(v -> {
            if (running) pauseTimer();
            else startTimer();
        });
        btnReset.setOnClickListener(v -> resetTimer());
        updateUi();
    }

    private void switchMode(Mode nextMode) {
        mode = nextMode;
        durationMs = durationForMode(mode);
        remainingMs = durationMs;
        stopTimer();
        updateUi();
    }

    private void startTimer() {
        running = true;
        btnStartPause.setText("Pause");
        timer = new CountDownTimer(remainingMs, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingMs = millisUntilFinished;
                updateUi();
            }

            @Override
            public void onFinish() {
                remainingMs = 0L;
                running = false;
                if (mode == Mode.FOCUS) {
                    focusSessions++;
                    switchMode(focusSessions % 4 == 0 ? Mode.LONG_BREAK : Mode.SHORT_BREAK);
                } else {
                    switchMode(Mode.FOCUS);
                }
            }
        }.start();
    }

    private void pauseTimer() {
        stopTimer();
        updateUi();
    }

    private void resetTimer() {
        remainingMs = durationMs;
        stopTimer();
        updateUi();
    }

    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        running = false;
        if (btnStartPause != null) {
            btnStartPause.setText("Start");
        }
    }

    private void updateUi() {
        if (tvTime == null) return;
        long totalSeconds = Math.max(0L, remainingMs / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        tvTime.setText(String.format(Locale.US, "%02d:%02d", minutes, seconds));
        progress.setMax(Math.max(1, (int) (durationMs / 1000L)));
        progress.setProgressCompat((int) Math.max(0L, remainingMs / 1000L), false);
        tvRounds.setText(String.format(Locale.US, "Today: %d %s",
                focusSessions,
                focusSessions == 1 ? "session" : "sessions"));
        long focusMinutes = focusSessions * Math.max(1L, Math.round(focusDurationMs / 60000f));
        tvFocusTime.setText(String.format(Locale.US, "Focus time: %dh %02dm",
                focusMinutes / 60L,
                focusMinutes % 60L));
        tvGoal.setText(String.format(Locale.US, "Goal: %d sessions", DAILY_GOAL));

        if (mode == Mode.FOCUS) {
            tvSubtitle.setText("Ready to focus");
        } else if (mode == Mode.SHORT_BREAK) {
            tvSubtitle.setText(String.format(Locale.US, "Recharge for %d minutes", shortBreakDurationMs / 60000L));
        } else {
            tvSubtitle.setText("Take a longer reset");
        }
        btnStartPause.setText(running ? "Pause" : "Start");
        updateModeTabs();
    }

    private void showTaskPickerPlaceholder() {
        Toast.makeText(requireContext(), "Task picker will be connected later", Toast.LENGTH_SHORT).show();
    }

    private void showDurationDialog() {
        if (running) {
            Toast.makeText(requireContext(), "Pause the timer before changing time", Toast.LENGTH_SHORT).show();
            return;
        }

        EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setSelectAllOnFocus(true);
        input.setText(String.valueOf(Math.max(1L, durationMs / 60000L)));

        new AlertDialog.Builder(requireContext())
                .setTitle("Set " + modeTitle(mode) + " minutes")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dialog, which) -> {
                    String value = input.getText().toString().trim();
                    if (value.isEmpty()) {
                        Toast.makeText(requireContext(), "Enter minutes first", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int minutes = Integer.parseInt(value);
                    if (minutes < 1 || minutes > 180) {
                        Toast.makeText(requireContext(), "Use 1-180 minutes", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    setDurationForMode(mode, minutes * 60 * 1000L);
                    durationMs = durationForMode(mode);
                    remainingMs = durationMs;
                    stopTimer();
                    updateUi();
                })
                .show();
    }

    private void updateModeTabs() {
        updateTab(tabFocus, mode == Mode.FOCUS);
        updateTab(tabShortBreak, mode == Mode.SHORT_BREAK);
        updateTab(tabLongBreak, mode == Mode.LONG_BREAK);
    }

    private void updateTab(TextView tab, boolean selected) {
        if (tab == null) return;
        tab.setBackgroundResource(selected ? R.drawable.bg_pomodoro_tab_selected : 0);
        tab.setTextColor(ContextCompat.getColor(requireContext(),
                selected ? R.color.colorAddAction : R.color.colorOnSurfaceVariant));
        tab.setTypeface(Typeface.DEFAULT, selected ? Typeface.BOLD : Typeface.NORMAL);
        if (!selected) {
            tab.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    private long durationForMode(Mode mode) {
        if (mode == Mode.FOCUS) return focusDurationMs;
        if (mode == Mode.SHORT_BREAK) return shortBreakDurationMs;
        return longBreakDurationMs;
    }

    private void setDurationForMode(Mode mode, long durationMs) {
        if (mode == Mode.FOCUS) {
            focusDurationMs = durationMs;
        } else if (mode == Mode.SHORT_BREAK) {
            shortBreakDurationMs = durationMs;
        } else {
            longBreakDurationMs = durationMs;
        }
    }

    private String modeTitle(Mode mode) {
        if (mode == Mode.FOCUS) return "Pomodoro";
        if (mode == Mode.SHORT_BREAK) return "short break";
        return "long break";
    }

    @Override
    public void onDestroyView() {
        stopTimer();
        super.onDestroyView();
    }
}
