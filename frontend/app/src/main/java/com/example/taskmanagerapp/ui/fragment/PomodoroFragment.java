package com.example.taskmanagerapp.ui.fragment;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.taskmanagerapp.R;
import com.example.taskmanagerapp.ui.activity.MainActivity;

import java.util.Locale;

public class PomodoroFragment extends Fragment {
    private static final long FOCUS_MS = 25 * 60 * 1000L;
    private static final long SHORT_BREAK_MS = 5 * 60 * 1000L;
    private static final long LONG_BREAK_MS = 15 * 60 * 1000L;

    private TextView tvMode;
    private TextView tvSubtitle;
    private TextView tvTime;
    private TextView tvRounds;
    private TextView btnStartPause;
    private ProgressBar progress;
    private CountDownTimer timer;
    private long durationMs = FOCUS_MS;
    private long remainingMs = FOCUS_MS;
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
        tvMode = view.findViewById(R.id.tvPomodoroMode);
        tvSubtitle = view.findViewById(R.id.tvPomodoroSubtitle);
        tvTime = view.findViewById(R.id.tvPomodoroTime);
        tvRounds = view.findViewById(R.id.tvPomodoroRounds);
        btnStartPause = view.findViewById(R.id.btnPomodoroStartPause);
        progress = view.findViewById(R.id.progressPomodoro);
        Button btnFocus = view.findViewById(R.id.btnPomodoroFocus);
        Button btnShortBreak = view.findViewById(R.id.btnPomodoroShortBreak);
        Button btnLongBreak = view.findViewById(R.id.btnPomodoroLongBreak);
        Button btnReset = view.findViewById(R.id.btnPomodoroReset);

        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).showSimpleToolbar(getString(R.string.title_pomodoro));
        }

        btnFocus.setOnClickListener(v -> switchMode(Mode.FOCUS));
        btnShortBreak.setOnClickListener(v -> switchMode(Mode.SHORT_BREAK));
        btnLongBreak.setOnClickListener(v -> switchMode(Mode.LONG_BREAK));
        btnStartPause.setOnClickListener(v -> {
            if (running) pauseTimer();
            else startTimer();
        });
        btnReset.setOnClickListener(v -> resetTimer());
        updateUi();
    }

    private void switchMode(Mode nextMode) {
        mode = nextMode;
        if (mode == Mode.FOCUS) durationMs = FOCUS_MS;
        else if (mode == Mode.SHORT_BREAK) durationMs = SHORT_BREAK_MS;
        else durationMs = LONG_BREAK_MS;
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
        progress.setMax((int) (durationMs / 1000L));
        progress.setProgress((int) Math.max(0L, remainingMs / 1000L));
        tvRounds.setText(focusSessions + (focusSessions == 1 ? " focus session today" : " focus sessions today"));

        if (mode == Mode.FOCUS) {
            tvMode.setText("Focus");
            tvSubtitle.setText("Study session");
        } else if (mode == Mode.SHORT_BREAK) {
            tvMode.setText("Short Break");
            tvSubtitle.setText("Recharge for 5 minutes");
        } else {
            tvMode.setText("Long Break");
            tvSubtitle.setText("Take a longer reset");
        }
        btnStartPause.setText(running ? "Pause" : "Start");
    }

    @Override
    public void onDestroyView() {
        stopTimer();
        super.onDestroyView();
    }
}
